package com.utsab.chess_engine.core.transposition;

import com.utsab.chess_engine.core.board.Move;

/**
 * Represents an entry in the transposition table, storing a 64-bit Zobrist key and a 64-bit value encoding
 * the evaluation score, best move, hash flag, and search depth. Used to cache search results in a UCI-compatible
 * chess engine, reducing redundant evaluations for positions reached via different move orders.
 * <p>
 * The value encoding structure is:
 * <ul>
 *     <li>BitboardConstants 32-63: Score (-1,000,000 to 1,000,000, including checkmate scores)</li>
 *     <li>BitboardConstants 16-31: MoveEntity (16-bit move encoding, see {@link Move})</li>
 *     <li>BitboardConstants 12-15: Flag (0-2 for {@link HashFlag} values, plus padding)</li>
 *     <li>BitboardConstants 0-11: Depth (0-265, max depth 256)</li>
 * </ul>
 *
 * @param key the 64-bit Zobrist key identifying the position
 * @param value the 64-bit encoded value containing score, move, flag, and depth
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Transposition_Table">Chess Programming Wiki - Transposition Table</a>
 */
public record HashEntry(long key, long value) {

    /**
     * Mask to clear the score bits (bits 32-63) in the value.
     */
    private static final long CLEAR_SCORE_MASK = 0xffffffffL;

    /**
     * Extracts the evaluation score from the entry.
     *
     * @return the score, ranging from -1,000,000 to 1,000,000, including checkmate scores
     */
    public int getScore() {
        long score = value >>> 32;
        return (int) score;
    }

    /**
     * Extracts the best move stored in the entry, if any.
     *
     * @return the move, or null if no move is stored
     */
    public Move getMove() {
        long move = (value >> 16) & 0xffff;
        return move > 0 ? new Move((short) move) : null;
    }

    /**
     * Extracts the hash flag indicating the score's type (exact, lower bound, or upper bound).
     *
     * @return the {@link HashFlag} value
     */
    public HashFlag getFlag() {
        long flag = (value >>> 12) & 0xf;
        return HashFlag.valueOf((int) flag);
    }

    /**
     * Extracts the search depth at which this entry was created.
     *
     * @return the depth (0-265)
     */
    public int getDepth() {
        return (int) value & 0x0fff;
    }

    /**
     * Creates a new HashEntry with the specified parameters.
     *
     * @param zobristKey the Zobrist key for the position
     * @param score the evaluation score
     * @param move the best move, or null
     * @param flag the hash flag
     * @param depth the search depth
     * @return a new HashEntry instance
     */
    public static HashEntry of(long zobristKey, int score, Move move, HashFlag flag, int depth) {
        long moveValue = move != null ? move.getValue() : 0;
        long flagValue = HashFlag.value(flag);
        long value = (long) score << 32 | moveValue << 16 | flagValue << 12 | depth;
        return new HashEntry(zobristKey, value);
    }

    /**
     * Creates a new HashEntry with an updated score, preserving other fields.
     *
     * @param entry the original entry
     * @param score the new score
     * @return a new HashEntry with the updated score
     */
    public static HashEntry withScore(HashEntry entry, int score) {
        long value = (entry.value() & CLEAR_SCORE_MASK) | (long) score << 32;
        return new HashEntry(entry.key(), value);
    }
}