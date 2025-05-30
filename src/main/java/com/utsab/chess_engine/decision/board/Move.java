package com.utsab.chess_engine.decision.board;

/**
 * Represents a chess move with a starting and ending square.
 */
public record Move(int fromSquare, int toSquare) {
    /**
     * Constructs a Move with the specified from and to squares.
     *
     * @param fromSquare the starting square (0-63)
     * @param toSquare   the destination square (0-63)
     * @throws IllegalArgumentException if squares are out of bounds
     */
    public Move {
        if (fromSquare < 0 || fromSquare > 63 || toSquare < 0 || toSquare > 63) {
            throw new IllegalArgumentException("Square indices must be between 0 and 63: " + fromSquare + ", " + toSquare);
        }
    }

    /**
     * Returns the move in UCI notation (e.g., "e2e4").
     *
     * @return the move as a string
     */
    @Override
    public String toString() {
        String[] files = {"a", "b", "c", "d", "e", "f", "g", "h"};
        int fromFile = fromSquare % 8;
        int fromRank = fromSquare / 8 + 1;
        int toFile = toSquare % 8;
        int toRank = toSquare / 8 + 1;
        return files[fromFile] + fromRank + files[toFile] + toRank;
    }

}