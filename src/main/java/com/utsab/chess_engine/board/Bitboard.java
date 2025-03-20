package com.utsab.chess_engine.board;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Bitboard {
    private static final Logger logger = Logger.getLogger(Bitboard.class.getName());
    private long[] pieceBitboards; // 0-5: white P,N,B,R,Q,K; 6-11: black p,n,b,r,q,k
    private long occupied; // All occupied squares
    private long whitePieces; // All white pieces
    private long blackPieces; // All black pieces

    public Bitboard() {
        pieceBitboards = new long[12];
        occupied = 0L;
        whitePieces = 0L;
        blackPieces = 0L;
    }

    public long[] getPieceBitboards() {
        return pieceBitboards;
    }

    public long getOccupied() {
        return occupied;
    }

    public long getWhitePieces() {
        return whitePieces;
    }

    public long getBlackPieces() {
        return blackPieces;
    }

    public void setPiece(int pieceIndex, int square) {
        pieceBitboards[pieceIndex] |= (1L << square);
        occupied |= (1L << square);
        if (pieceIndex < 6) whitePieces |= (1L << square);
        else blackPieces |= (1L << square);
    }

    public void clearPiece(int pieceIndex, int square) {
        pieceBitboards[pieceIndex] &= ~(1L << square);
        occupied &= ~(1L << square);
        if (pieceIndex < 6) whitePieces &= ~(1L << square);
        else blackPieces &= ~(1L << square);
    }

    public boolean isSquareEmpty(int square) {
        return (occupied & (1L << square)) == 0;
    }

    public long getPieceBitboard(int pieceIndex) {
        return pieceBitboards[pieceIndex];
    }

    public void logPosition() {
        StringBuilder position = new StringBuilder("Current Board Position:\n");
        for (int rank = 7; rank >= 0; rank--) {
            position.append(rank + 1).append(" ");
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
        position.append("   a b c d e f g h\n");
        if (logger.isLoggable(Level.INFO)) {
            logger.info(position.toString());
        }
    }

    private char getPieceSymbol(int index) {
        return "♙♘♗♖♕♔♟♞♝♜♛♚".charAt(index);
    }
}