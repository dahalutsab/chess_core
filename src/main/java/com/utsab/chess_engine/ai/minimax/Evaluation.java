package com.utsab.chess_engine.ai.minimax;

import com.utsab.chess_engine.board.Bitboard;

public class Evaluation {
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 300;
    private static final int BISHOP_VALUE = 300;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;

    /**
     * Evaluates the board position based on material advantage.
     */
    public static int evaluate(Bitboard board) {
        int score = 0;
        score += Long.bitCount(board.getPieceBitboard(0)) * PAWN_VALUE;   // White pawns
        score += Long.bitCount(board.getPieceBitboard(1)) * KNIGHT_VALUE; // White knights
        score += Long.bitCount(board.getPieceBitboard(2)) * BISHOP_VALUE; // White bishops
        score += Long.bitCount(board.getPieceBitboard(3)) * ROOK_VALUE;   // White rooks
        score += Long.bitCount(board.getPieceBitboard(4)) * QUEEN_VALUE;  // White queens

        score -= Long.bitCount(board.getPieceBitboard(6)) * PAWN_VALUE;   // Black pawns
        score -= Long.bitCount(board.getPieceBitboard(7)) * KNIGHT_VALUE; // Black knights
        score -= Long.bitCount(board.getPieceBitboard(8)) * BISHOP_VALUE; // Black bishops
        score -= Long.bitCount(board.getPieceBitboard(9)) * ROOK_VALUE;   // Black rooks
        score -= Long.bitCount(board.getPieceBitboard(10)) * QUEEN_VALUE; // Black queens

        return score;
    }
}