package com.utsab.chess_engine.core.chess.transposition;

import com.utsab.chess_engine.core.chess.board.Move;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.Objects;

/**
 * A transposition table that stores search results for previously evaluated chess positions, including evaluation scores,
 * best moveEntities, search depths, and hash flags. By caching results, it reduces redundant evaluations for positions reached
 * via different move orders, significantly optimizing the search tree in a UCI-compatible chess engine.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Transposition_Table">Chess Programming Wiki - Transposition Table</a>
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TranspositionTable {

    /**
     * Size of each hash entry in bytes (32 bytes).
     */
    static final int ENTRY_SIZE_BYTES = 32;

    /**
     * Threshold for checkmate scores (±1,000,000 - 256).
     */
    static final int CHECKMATE_BOUND = 1_000_000 - 256;

    /**
     * Number of entries in the table, calculated from memory size.
     */
    final int tableSize;

    /**
     * Array of hash entries, indexed by Zobrist key.
     */
    HashEntry[] entries;

    /**
     * Number of table lookup attempts.
     */
    int tries;

    /**
     * Number of successful table hits.
     */
    int hits;

    /**
     * Constructs a transposition table with the specified size.
     *
     * @param tableSizeMb the table size in megabytes
     */
    public TranspositionTable(int tableSizeMb) {
        this.tableSize = (tableSizeMb * 1024 * 1024) / ENTRY_SIZE_BYTES;
        entries = new HashEntry[tableSize];
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
        tries++;
        for (int i = 0; i < 4; i++) {
            HashEntry entry = entries[index + i];
            if (entry != null && entry.key() == zobristKey) {
                hits++;
                if (isMateScore(entry.getScore())) {
                    int score = retrieveMateScore(entry.getScore(), ply);
                    entry = HashEntry.withScore(entry, score);
                }
                return entry;
            }
        }
        return null;
    }

    /**
     * Stores a new hash entry in the table, using a replacement strategy based on depth.
     *
     * @param zobristKey the Zobrist key of the position
     * @param flag the hash flag
     * @param depth the search depth
     * @param ply the current ply from the root
     * @param move the best move, or null
     * @param score the evaluation score
     */
    public void put(long zobristKey, HashFlag flag, int depth, int ply, Move move, int score) {
        int startIndex = getIndex(zobristKey);
        if (isMateScore(score)) score = calculateMateScore(score, ply);
        HashEntry newEntry = HashEntry.of(zobristKey, score, move, flag, depth);

        int replacedIndex = -1;
        int minDepth = Integer.MAX_VALUE;

        for (int i = startIndex; i < startIndex + 4; i++) {
            HashEntry storedEntry = entries[i];

            if (storedEntry == null || storedEntry.key() == zobristKey) {
                if (storedEntry == null || depth >= storedEntry.getDepth()) {
                    if (newEntry.getMove() == null && storedEntry != null && storedEntry.getMove() != null) {
                        newEntry = HashEntry.of(newEntry.key(), newEntry.getScore(), storedEntry.getMove(), newEntry.getFlag(), newEntry.getDepth());
                    }
                    replacedIndex = i;
                    break;
                } else {
                    return;
                }
            }

            if (storedEntry.getDepth() < minDepth) {
                minDepth = storedEntry.getDepth();
                replacedIndex = i;
            }
        }

        if (replacedIndex != -1) {
            entries[replacedIndex] = newEntry;
        }
    }

    /**
     * Clears the hash table, resetting entries and statistics.
     *
     */
    public void clear() {
        printStatistics();
        tries = 0;
        hits = 0;
        entries = new HashEntry[tableSize];
    }

    /**
     * Computes the table index for a given Zobrist key, ensuring alignment for bucketed entries.
     *
     * @param zobristKey the Zobrist key
     * @return the starting index for the bucket
     */
    private int getIndex(long zobristKey) {
        long index = (zobristKey ^ (zobristKey >>> 32)) & 0x7FFFFFFF;
        return (int) (index % (tableSize - 3)) & ~3;
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
     * Prints statistics about table usage, including fill percentage and hit rate.
     *
     */
    public void printStatistics() {
        long fill = Arrays.stream(entries).filter(Objects::nonNull).count();
        float fillPercentage = ((float) fill / tableSize) * 100;
        float hitPercentage = tries > 0 ? ((float) hits / tries) * 100 : 0;
        //System.out.printf("TT %s -- size: %d / %d (%.2f%%), tries: %d, hits: %d (%.2f%%)%n",
        //    this.hashCode(), fill, entries.length, fillPercentage, tries, hits, hitPercentage);
    }
}