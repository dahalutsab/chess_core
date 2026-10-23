package com.utsab.chess_engine.core.transposition;

import com.utsab.chess_engine.core.board.Move;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;

/**
 * A transposition table that stores search results for previously evaluated chess positions, including evaluation scores,
 * best moveEntities, search depths, and hash flags. By caching results, it reduces redundant evaluations for positions reached
 * via different move orders, significantly optimizing the search tree in a UCI-compatible chess engine.
 * <p>
 * Entries are stored in a flat primitive array rather than as objects, so storing a result never allocates. Each
 * entry's key and value are interleaved, so that a whole bucket sits in one or two adjacent cache lines and a probe
 * costs a single memory access. The table is shared lock-free between Lazy SMP threads: each key is stored
 * XOR-ed with its value, so a torn write from a concurrent thread simply fails verification and reads as a miss.
 *
 * @author Utsab Dahal
 * @version 1.1
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Transposition_Table">Chess Programming Wiki - Transposition Table</a>
 * @see <a href="https://www.chessprogramming.org/Shared_Hash_Table#Lock-less">Chess Programming Wiki - Lock-less hashing</a>
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TranspositionTable {

    /**
     * Size of each hash entry in bytes (8-byte key + 8-byte value + 1-byte age).
     */
    static final int ENTRY_SIZE_BYTES = 17;

    /**
     * Number of entries probed per bucket.
     */
    static final int BUCKET_SIZE = 4;

    /**
     * Threshold for checkmate scores (±1,000,000 - 256).
     */
    static final int CHECKMATE_BOUND = 1_000_000 - 256;

    /**
     * Number of entries in the table, calculated from memory size and rounded down to a whole number of buckets.
     */
    final int tableSize;

    /**
     * Number of buckets in the table.
     */
    final int bucketCount;

    /**
     * Interleaved entries: at {@code 2 * i} the Zobrist key XOR-ed with the value (for lock-less verification), and at
     * {@code 2 * i + 1} the packed entry value (score, move, flag, depth), see {@link HashEntry}.
     */
    final long[] entries;

    /**
     * The search generation in which each entry was last written. Entries from older searches are replaced first.
     */
    final byte[] ages;

    /**
     * The current search generation.
     */
    volatile byte currentAge;

    /**
     * Constructs a transposition table with the specified size.
     *
     * @param tableSizeMb the table size in megabytes
     */
    public TranspositionTable(int tableSizeMb) {
        long entries = ((long) tableSizeMb * 1024 * 1024) / ENTRY_SIZE_BYTES;
        this.bucketCount = (int) Math.max(1, Math.min(entries, Integer.MAX_VALUE - BUCKET_SIZE) / BUCKET_SIZE);
        this.tableSize = bucketCount * BUCKET_SIZE;
        this.entries = new long[2 * tableSize];
        this.ages = new byte[tableSize];
    }

    /**
     * Retrieves a hash entry for the given Zobrist key, adjusting mate scores based on ply.
     *
     * @param zobristKey the Zobrist key of the position
     * @param ply the current ply from the root
     * @return the corresponding {@link HashEntry}, or null if not found
     */
    public HashEntry get(long zobristKey, int ply) {
        int index = getIndex(zobristKey);
        for (int i = index; i < index + BUCKET_SIZE; i++) {
            long value = entries[2 * i + 1];
            if (value != 0 && (entries[2 * i] ^ value) == zobristKey) {
                HashEntry entry = new HashEntry(zobristKey, value);
                if (isMateScore(entry.getScore())) {
                    entry = HashEntry.withScore(entry, retrieveMateScore(entry.getScore(), ply));
                }
                return entry;
            }
        }
        return null;
    }

    /**
     * Stores a new hash entry in the table, using a replacement strategy based on depth and age.
     *
     * @param zobristKey the Zobrist key of the position
     * @param flag the hash flag
     * @param depth the search depth
     * @param ply the current ply from the root
     * @param move the best move found, or null
     * @param score the evaluation score
     */
    public void put(long zobristKey, HashFlag flag, int depth, int ply, Move move, int score) {
        int startIndex = getIndex(zobristKey);
        if (isMateScore(score)) score = calculateMateScore(score, ply);
        byte age = currentAge;

        int replacedIndex = startIndex;
        int worstPriority = Integer.MAX_VALUE;

        for (int i = startIndex; i < startIndex + BUCKET_SIZE; i++) {
            long storedValue = entries[2 * i + 1];

            if (storedValue == 0) {
                replacedIndex = i;
                break;
            }

            if ((entries[2 * i] ^ storedValue) == zobristKey) {
                HashEntry storedEntry = new HashEntry(zobristKey, storedValue);
                // Keep a deeper result from the current search for the same position
                if (depth < storedEntry.getDepth() && ages[i] == age && flag != HashFlag.EXACT) {
                    return;
                }
                // Preserve the stored best move if we don't have one
                if (move == null) {
                    move = storedEntry.getMove();
                }
                replacedIndex = i;
                break;
            }

            // Prefer replacing entries from previous searches, then the shallowest entries
            int priority = new HashEntry(0, storedValue).getDepth() + (ages[i] == age ? 1024 : 0);
            if (priority < worstPriority) {
                worstPriority = priority;
                replacedIndex = i;
            }
        }

        long value = HashEntry.of(zobristKey, score, move, flag, depth).value();
        entries[2 * replacedIndex + 1] = value;
        entries[2 * replacedIndex] = zobristKey ^ value;
        ages[replacedIndex] = age;
    }

    /**
     * Marks the start of a new search, so that entries written by previous searches become preferred for replacement.
     */
    public void incrementAge() {
        currentAge++;
    }

    /**
     * Clears the hash table, resetting all entries.
     *
     */
    public void clear() {
        Arrays.fill(entries, 0L);
        Arrays.fill(ages, (byte) 0);
        currentAge = 0;
    }

    /**
     * Computes the table index for a given Zobrist key, ensuring alignment for bucketed entries.
     *
     * @param zobristKey the Zobrist key
     * @return the starting index for the bucket
     */
    private int getIndex(long zobristKey) {
        // Map the upper 32 bits of the key onto the bucket range without a modulo
        long bucket = ((zobristKey >>> 32) * bucketCount) >>> 32;
        return (int) bucket * BUCKET_SIZE;
    }

    /**
     * Checks if a score represents a checkmate or stalemate.
     *
     * @param score the evaluation score
     * @return true if the score is a mate score, false otherwise
     */
    private boolean isMateScore(int score) {
        return Math.abs(score) >= CHECKMATE_BOUND;
    }

    /**
     * Adjusts a mate score for storage, accounting for the ply from the root.
     *
     * @param score the original mate score
     * @param plyFromRoot the current ply
     * @return the adjusted score
     */
    private int calculateMateScore(int score, int plyFromRoot) {
        return score > 0 ? score - plyFromRoot : score + plyFromRoot;
    }

    /**
     * Retrieves a mate score, adjusting it based on the current ply.
     *
     * @param score the stored mate score
     * @param plyFromRoot the current ply
     * @return the adjusted score
     */
    private int retrieveMateScore(int score, int plyFromRoot) {
        return score > 0 ? score + plyFromRoot : score - plyFromRoot;
    }

    /**
     * Prints statistics about table usage, including fill percentage.
     *
     */
    public void printStatistics() {
        long fill = java.util.stream.IntStream.range(0, tableSize).filter(i -> entries[2 * i + 1] != 0).count();
        float fillPercentage = ((float) fill / tableSize) * 100;
        System.out.printf("TT %s -- size: %d / %d (%.2f%%)%n", this.hashCode(), fill, tableSize, fillPercentage);
    }
}
