package com.utsab.chess_engine.core.chess.utils;

import java.util.Arrays;

/**
 * Provides precalculated lookup tables for distances between squares on a chessboard, including Chebyshev,
 * Manhattan, and center Manhattan distances. Used to optimize evaluation and move generation in a
 * UCI-compatible chess engine.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Distance">Chess Programming Wiki - Distance</a>
 */
public class Distance {

    /**
     * Lookup table for Chebyshev distances (maximum of rank and file differences) between squares.
     */
    private static final int[][] CHEBYSHEV_DISTANCE = new int[64][64];

    /**
     * Lookup table for Manhattan distances (sum of rank and file differences) between squares.
     */
    private static final int[][] MANHATTAN_DISTANCE = new int[64][64];

    /**
     * Lookup table for Manhattan distances from each square to the closest central square (D4, D5, E4, E5).
     */
    private static final int[] CENTER_MANHATTAN_DISTANCE = new int[64];

    /**
     * Returns the Chebyshev distance (number of king moveEntities) between two squares.
     *
     * @param sq1 the first square index (0-63)
     * @param sq2 the second square index (0-63)
     * @return the Chebyshev distance
     */
    public static int chebyshev(int sq1, int sq2) {
        return CHEBYSHEV_DISTANCE[sq1][sq2];
    }

    /**
     * Returns the Manhattan distance (sum of orthogonal moveEntities) between two squares.
     *
     * @param sq1 the first square index (0-63)
     * @param sq2 the second square index (0-63)
     * @return the Manhattan distance
     */
    public static int manhattan(int sq1, int sq2) {
        return MANHATTAN_DISTANCE[sq1][sq2];
    }

    /**
     * Returns the Manhattan distance from a square to the closest central square (D4, D5, E4, E5).
     *
     * @param sq the square index (0-63)
     * @return the center Manhattan distance
     */
    public static int centerManhattan(int sq) {
        return CENTER_MANHATTAN_DISTANCE[sq];
    }

    /*
      Initializes the distance lookup tables using precalculated values.
     */
    static {
        for (int sq1 = 0; sq1 < 64; sq1++) {
            for (int sq2 = 0; sq2 < 64; sq2++) {
                int file1 = sq1 & 7;
                int file2 = sq2 & 7;
                int rank1 = sq1 >>> 3;
                int rank2 = sq2 >>> 3;
                int rankDistance = Math.abs(rank2 - rank1);
                int fileDistance = Math.abs(file2 - file1);

                int chebyshev = Math.max(rankDistance, fileDistance);
                CHEBYSHEV_DISTANCE[sq1][sq2] = chebyshev;

                int manhattan = rankDistance + fileDistance;
                MANHATTAN_DISTANCE[sq1][sq2] = manhattan;
            }
            int[] centerManhattans = new int[]{
                    manhattan(sq1, 27), manhattan(sq1, 28), manhattan(sq1, 35), manhattan(sq1, 36)
            };
            Arrays.sort(centerManhattans);
            CENTER_MANHATTAN_DISTANCE[sq1] = centerManhattans[0];
        }
    }
}