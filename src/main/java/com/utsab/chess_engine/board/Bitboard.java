package com.utsab.chess_engine.board;

import java.util.logging.Logger;

public class Bitboard {
    private static final Logger logger = Logger.getLogger(Bitboard.class.getName());
    private long[] pieceBitboards; // 0-5: white P,N,B,R,Q,K; 6-11: black p,n,b,r,q,k
    private long occupied;        // All occupied squares
    private long whitePieces;     // All white pieces
    private long blackPieces;     // All black pieces
    private boolean whiteToMove;  // Whose turn it is

    public Bitboard() {
        pieceBitboards = new long[12];
        occupied = 0L;
        whitePieces = 0L;
        blackPieces = 0L;
        whiteToMove = true; // White moves first
    }

    // Getters
    public long getPieceBitboard(int index) { return pieceBitboards[index]; }
    public long getOccupied() { return occupied; }
    public long getWhitePieces() { return whitePieces; }
    public long getBlackPieces() { return blackPieces; }
    public boolean isWhiteToMove() { return whiteToMove; }

    // Setter for whiteToMove (used by FenParser)
    public void setWhiteToMove(boolean whiteToMove) {
        this.whiteToMove = whiteToMove;
        logger.info("Set whiteToMove to: " + whiteToMove);
    }

    // NEW: Method to set a bit in a specific piece bitboard
    public void setPieceBitboard(int index, long value) {
        pieceBitboards[index] = value;
    }

    // NEW: Method to update occupied, whitePieces, and blackPieces directly
    public void setOccupied(long occupied) { this.occupied = occupied; }
    public void setWhitePieces(long whitePieces) { this.whitePieces = whitePieces; }
    public void setBlackPieces(long blackPieces) { this.blackPieces = blackPieces; }

    // Record to store move information for undoing moves
    public record MoveInfo(int movingPieceIndex, int fromSquare, int toSquare, int capturedPieceIndex, boolean previousWhiteToMove) {}

    public MoveInfo makeMove(Move move) {
        int from = move.fromSquare();
        int to = move.toSquare();
        int movingPieceIndex = getPieceIndexAtSquare(from);
        int capturedPieceIndex = getPieceIndexAtSquare(to);
        boolean previousWhiteToMove = whiteToMove;

        if (movingPieceIndex == -1) {
            throw new IllegalStateException("No piece on from square: " + from);
        }

        // Clear moving piece from fromSquare
        pieceBitboards[movingPieceIndex] &= ~(1L << from);
        occupied &= ~(1L << from);
        if (movingPieceIndex < 6) whitePieces &= ~(1L << from);
        else blackPieces &= ~(1L << from);

        // Handle capture
        if (capturedPieceIndex != -1) {
            pieceBitboards[capturedPieceIndex] &= ~(1L << to);
            if (capturedPieceIndex < 6) whitePieces &= ~(1L << to);
            else blackPieces &= ~(1L << to);
            occupied &= ~(1L << to);
        }

        // Set piece on toSquare
        pieceBitboards[movingPieceIndex] |= (1L << to);
        occupied |= (1L << to);
        if (movingPieceIndex < 6) whitePieces |= (1L << to);
        else blackPieces |= (1L << to);

        whiteToMove = !whiteToMove; // Switch turn
        logger.info("Made move: " + move + ", new board state:\n" + this);
        return new MoveInfo(movingPieceIndex, from, to, capturedPieceIndex, previousWhiteToMove);
    }

    public void undoMove(MoveInfo moveInfo) {
        int movingPieceIndex = moveInfo.movingPieceIndex();
        int from = moveInfo.fromSquare();
        int to = moveInfo.toSquare();
        int capturedPieceIndex = moveInfo.capturedPieceIndex();

        // Clear piece from toSquare
        pieceBitboards[movingPieceIndex] &= ~(1L << to);
        occupied &= ~(1L << to);
        if (movingPieceIndex < 6) whitePieces &= ~(1L << to);
        else blackPieces &= ~(1L << to);

        // Restore piece to fromSquare
        pieceBitboards[movingPieceIndex] |= (1L << from);
        occupied |= (1L << from);
        if (movingPieceIndex < 6) whitePieces |= (1L << from);
        else blackPieces |= (1L << from);

        // Restore captured piece, if any
        if (capturedPieceIndex != -1) {
            pieceBitboards[capturedPieceIndex] |= (1L << to);
            occupied |= (1L << to);
            if (capturedPieceIndex < 6) whitePieces |= (1L << to);
            else blackPieces |= (1L << to);
        }

        whiteToMove = moveInfo.previousWhiteToMove(); // Restore turn
        logger.info("Undid move, restores board state:\n" + this);
    }

    private int getPieceIndexAtSquare(int square) {
        for (int i = 0; i < 12; i++) {
            if ((pieceBitboards[i] & (1L << square)) != 0) {
                return i;
            }
        }
        return -1; // No piece at square
    }

    public boolean isInCheck(boolean white) {
        // TODO: Implement check detection
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            sb.append(rank + 1).append(" ");
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                char piece = '.';
                for (int i = 0; i < 12; i++) {
                    if ((pieceBitboards[i] & (1L << square)) != 0) {
                        piece = "PNBRQKpnbrqk".charAt(i);
                        break;
                    }
                }
                sb.append(piece).append(" ");
            }
            sb.append("\n");
        }
        sb.append("  a b c d e f g h");
        return sb.toString();
    }
}