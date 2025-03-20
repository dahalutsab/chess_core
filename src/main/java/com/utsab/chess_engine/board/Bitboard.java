package com.utsab.chess_engine.board;

import lombok.Getter;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Bitboard {
    Logger logger = Logger.getLogger(getClass().getName());

    private final long[] pieceBitboards; // Bitboards for each piece type
    @Getter
    private long occupied; // Bitboard of all occupied squares

    public Bitboard() {
        pieceBitboards = new long[12]; // 6 white pieces + 6 black pieces
        occupied = 0L;
    }

    public void setPiece(int pieceIndex, int square) {
        pieceBitboards[pieceIndex] |= 1L << square;
        occupied |= 1L << square;
    }

    public void clearPiece(int pieceIndex, int square) {
        pieceBitboards[pieceIndex] &= ~(1L << square);
        occupied &= ~(1L << square);
    }

    public boolean isPieceAt(int pieceIndex, int square) {
        return (pieceBitboards[pieceIndex] & (1L << square)) != 0;
    }

    public boolean isSquareEmpty(int square) {
        return (occupied & (1L << square)) == 0;
    }

    public long getPieceBitboard(int pieceIndex) {
        return pieceBitboards[pieceIndex];
    }

    public void logPosition() {
        StringBuilder position = new StringBuilder("Current Position:\n");
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                char piece = '.';
                for (int i = 0; i < 12; i++) {
                    if ((pieceBitboards[i] & (1L << square)) != 0) {
                        piece = getPieceSymbol(i);
                        break;
                    }
                }
                position.append(piece).append(" ");
            }
            position.append("\n");
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.info(position.toString());
        }
    }

    private char getPieceSymbol(int index) {
        return "♙♘♗♖♕♔♟♞♝♜♛♚".charAt(index);
    }
}