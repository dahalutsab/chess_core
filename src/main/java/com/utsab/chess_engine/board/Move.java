package com.utsab.chess_engine.board;

/**
 * Represents a chess move with a starting and ending square.
 */
public final class Move {
    private final int fromSquare;
    private final int toSquare;

    /**
     * Constructs a Move with the specified from and to squares.
     *
     * @param fromSquare the starting square (0-63)
     * @param toSquare   the destination square (0-63)
     * @throws IllegalArgumentException if squares are out of bounds
     */
    public Move(int fromSquare, int toSquare) {
        if (fromSquare < 0 || fromSquare > 63 || toSquare < 0 || toSquare > 63) {
            throw new IllegalArgumentException("Square indices must be between 0 and 63: " + fromSquare + ", " + toSquare);
        }
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
    }

    public int fromSquare() { return fromSquare; }
    public int toSquare() { return toSquare; }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return fromSquare == move.fromSquare && toSquare == move.toSquare;
    }

    @Override
    public int hashCode() {
        return 31 * fromSquare + toSquare;
    }
}