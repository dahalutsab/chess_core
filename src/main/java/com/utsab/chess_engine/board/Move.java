package com.utsab.chess_engine.board;

public record Move(int fromSquare, int toSquare) {
    @Override
    public String toString() {
        return "[" + fromSquare + " -> " + toSquare + "]";
    }
}