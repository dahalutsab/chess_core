package com.utsab.chess_engine.core.transposition;

/**
 * Enumerates flags used in transposition table entries to indicate the type of evaluation score stored.
 * These flags help determine whether a score is exact, a lower bound, or an upper bound, guiding the search
 * algorithm in a UCI-compatible chess engine.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Transposition_Table">Chess Programming Wiki - Transposition Table</a>
 */
public enum HashFlag {

    /**
     * Indicates that the stored score is the exact evaluation of the position, as determined by the evaluation function.
     */
    EXACT,

    /**
     * Indicates that the stored score is a lower bound. The position resulted in a beta-cutoff for the opponent,
     * meaning they discarded the move leading to this position because it was worse than other options. The score
     * represents the minimum evaluation possible, but better moveEntities may exist.
     */
    LOWER,

    /**
     * Indicates that the stored score is an upper bound. The position was evaluated as inferior to other alternatives,
     * meaning no move improves the player's alpha score. The score is the maximum achievable evaluation from this position.
     */
    UPPER;

    /**
     * Converts a HashFlag to its integer representation for storage in a transposition table entry.
     *
     * @param flag the HashFlag to convert
     * @return the integer value (0 for EXACT, 1 for LOWER, 2 for UPPER)
     */
    public static int value(HashFlag flag) {
        return switch (flag) {
            case EXACT -> 0;
            case LOWER -> 1;
            case UPPER -> 2;
        };
    }

    /**
     * Converts an integer value to the corresponding HashFlag.
     *
     * @param value the integer value (0-2)
     * @return the corresponding HashFlag
     * @throws IllegalArgumentException if the value is not 0, 1, or 2
     */
    public static HashFlag valueOf(int value) {
        return switch (value) {
            case 0 -> EXACT;
            case 1 -> LOWER;
            case 2 -> UPPER;
            default -> throw new IllegalArgumentException("Illegal node type " + value);
        };
    }
}