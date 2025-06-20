package com.utsab.chess_engine.core.transposition.pawn;

import java.util.Arrays;
import java.util.Objects;

/**
 * A hash table for storing pawn structure evaluations, indexed by pawn Zobrist keys. Each entry contains
 * midgame and endgame scores for both sides, optimizing pawn evaluation in a UCI-compatible chess engine.
 * Tracks statistics for table usage and hit rates.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Pawn_Hash_Table">Chess Programming Wiki - Pawn Hash Table</a>
 */
public class PawnHashTable {

    /**
     * The size of the hash table (250,000 entries).
     */
    static final int TABLE_SIZE = 250_000;

    /**
     * Array of pawn hash entries, indexed by pawn Zobrist key.
     */
    PawnHashEntry[] entries;

    /**
     * Number of table lookup attempts.
     */
    int tries;

    /**
     * Number of successful table hits.
     */
    int hits;

    /**
     * Constructs a new pawn hash table with the specified size.
     */
    public PawnHashTable() {
        entries = new PawnHashEntry[TABLE_SIZE];
    }

    /**
     * Retrieves a pawn hash entry for the given pawn Zobrist key.
     *
     * @param pawnKey the pawn Zobrist key
     * @return the corresponding {@link PawnHashEntry}, or null if not found
     */
    public PawnHashEntry get(long pawnKey) {
        int index = getIndex(pawnKey);
        tries++;
        PawnHashEntry entry = entries[index];
        if (entry != null && entry.key() == pawnKey) {
            hits++;
            return entry;
        }
        return null;
    }

    /**
     * Stores a pawn hash entry for the given pawn Zobrist key.
     *
     * @param pawnKey the pawn Zobrist key
     * @param entry the {@link PawnHashEntry} to store
     */
    public void put(long pawnKey, PawnHashEntry entry) {
        int index = getIndex(pawnKey);
        entries[index] = entry;
    }

    /**
     * Computes the table index for a given pawn Zobrist key.
     *
     * @param pawnKey the pawn Zobrist key
     * @return the index (0 to TABLE_SIZE-1)
     */
    private int getIndex(long pawnKey) {
        int index = Long.hashCode(pawnKey);
        if (index < 0) {
            index = -index;
        }
        index = index % TABLE_SIZE;
        return index;
    }

    /**
     * Clears the hash table, resetting entries and statistics.
     */
    public void clear() {
        tries = 0;
        hits = 0;
        entries = new PawnHashEntry[TABLE_SIZE];
    }

    /**
     * Prints statistics about table usage, including fill percentage and hit rate.
     **/
    public void printStatistics() {
        long fill = Arrays.stream(entries).filter(Objects::nonNull).count();
        float fillPercentage = ((float) fill / (float) TABLE_SIZE) * 100;
        float hitPercentage = tries > 0 ? ((float) hits / (float) tries) * 100 : 0;
        //System.out.printf("Pawn TT -- table size: %d / %d (%.2f%%), tries: %d, hits: %d (%.2f%%)%n",
        //    fill, TABLE_SIZE, fillPercentage, tries, hits, hitPercentage);
    }
}