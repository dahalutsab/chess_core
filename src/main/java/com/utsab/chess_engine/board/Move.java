package com.utsab.chess_engine.board;

public record Move(int fromSquare, int toSquare) {
    @Override
    public String toString() {
        return squareToString(fromSquare) + squareToString(toSquare);
    }

    private String squareToString(int square) {
        char file = (char) ('a' + (square % 8));
        int rank = (square / 8) + 1; // Square 0 = a1, 63 = h8
        return String.format("%c%d", file, rank);
    }
}