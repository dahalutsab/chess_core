package com.utsab.chess_engine.board;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MoveGenerator {
    private static final Logger logger = Logger.getLogger(MoveGenerator.class.getName());
    private Bitboard bitboard;
    private boolean whiteToMove;

    public MoveGenerator(Bitboard bitboard) {
        this.bitboard = bitboard;
        this.whiteToMove = true;
    }

    public Bitboard getBitboard() {
        return bitboard;
    }

    public boolean isWhiteToMove() {
        return whiteToMove;
    }

    public void setBitboard(Bitboard bitboard) {
        this.bitboard = bitboard;
    }

    public void setWhiteToMove(boolean whiteToMove) {
        this.whiteToMove = whiteToMove;
        logger.info("Set whiteToMove to: " + whiteToMove);
    }

    public List<Move> generateMoves() {
        logger.info("Generating moves for " + (whiteToMove ? "white" : "black"));
        List<Move> moves = new ArrayList<>();
        if (whiteToMove) {
            logger.info("Calling generatePawnMoves(true)");
            generatePawnMoves(moves, true);
            logger.info("Calling generateKnightMoves(1)");
            generateKnightMoves(moves, 1);
            logger.info("Calling generateBishopMoves(2)");
            generateBishopMoves(moves, 2);
            logger.info("Calling generateRookMoves(3)");
            generateRookMoves(moves, 3);
            logger.info("Calling generateQueenMoves(4)");
            generateQueenMoves(moves, 4);
            logger.info("Calling generateKingMoves(5)");
            generateKingMoves(moves, 5);
        } else {
            logger.info("Calling generatePawnMoves(false)");
            generatePawnMoves(moves, false);
            logger.info("Calling generateKnightMoves(7)");
            generateKnightMoves(moves, 7);
            logger.info("Calling generateBishopMoves(8)");
            generateBishopMoves(moves, 8);
            logger.info("Calling generateRookMoves(9)");
            generateRookMoves(moves, 9);
            logger.info("Calling generateQueenMoves(10)");
            generateQueenMoves(moves, 10);
            logger.info("Calling generateKingMoves(11)");
            generateKingMoves(moves, 11);
        }
        logValidMoves(moves);
        return moves;
    }

    private void generatePawnMoves(List<Move> moves, boolean isWhite) {
        long pawns = bitboard.getPieceBitboard(isWhite ? 0 : 6);
        long empty = ~bitboard.getOccupied();
        long targets = isWhite ? bitboard.getBlackPieces() : bitboard.getWhitePieces();

        int direction = isWhite ? 8 : -8;
        long singlePush = isWhite ? (pawns << 8) : (pawns >>> 8);
        long doublePush = isWhite ? ((pawns & 0xFF00L) << 16) : ((pawns & 0xFF000000000000L) >>> 16);

        long validSingle = singlePush & empty;
        while (validSingle != 0) {
            int to = Long.numberOfTrailingZeros(validSingle);
            moves.add(new Move(to - direction, to));
            validSingle &= validSingle - 1;
        }

        long validDouble = doublePush & empty & (isWhite ? (empty << 8) : (empty >>> 8));
        while (validDouble != 0) {
            int to = Long.numberOfTrailingZeros(validDouble);
            moves.add(new Move(to - 2 * direction, to));
            validDouble &= validDouble - 1;
        }

        long leftCapture = isWhite ? ((pawns & ~0x0101010101010100L) << 7) : ((pawns & ~0x0101010101010100L) >>> 9);
        long rightCapture = isWhite ? ((pawns & ~0x8080808080808080L) << 9) : ((pawns & ~0x8080808080808080L) >>> 7);
        validSingle = leftCapture & targets;
        while (validSingle != 0) {
            int to = Long.numberOfTrailingZeros(validSingle);
            moves.add(new Move(to - (isWhite ? 7 : -9), to));
            validSingle &= validSingle - 1;
        }
        validSingle = rightCapture & targets;
        while (validSingle != 0) {
            int to = Long.numberOfTrailingZeros(validSingle);
            moves.add(new Move(to - (isWhite ? 9 : -7), to));
            validSingle &= validSingle - 1;
        }
    }

    private void generateKnightMoves(List<Move> moves, int pieceIndex) {
        long knights = bitboard.getPieceBitboard(pieceIndex); // White knights: 0x42 (b1:1, g1:6)
        long notOwnPieces = ~(pieceIndex < 6 ? bitboard.getWhitePieces() : bitboard.getBlackPieces());
        logger.info("Knight bitboard for piece " + pieceIndex + ": " + Long.toHexString(knights)); // Debug

        while (knights != 0) {
            int from = Long.numberOfTrailingZeros(knights);
            long knightMoves = knightAttacks(from) & notOwnPieces;
            logger.info("Knight moves from " + from + ": " + Long.toHexString(knightMoves)); // Debug
            while (knightMoves != 0) {
                int to = Long.numberOfTrailingZeros(knightMoves);
                moves.add(new Move(from, to));
                knightMoves &= knightMoves - 1;
            }
            knights &= knights - 1;
        }
    }

    private void generateBishopMoves(List<Move> moves, int pieceIndex) {
        long bishops = bitboard.getPieceBitboard(pieceIndex);
        long notOwnPieces = ~(pieceIndex < 6 ? bitboard.getWhitePieces() : bitboard.getBlackPieces());

        while (bishops != 0) {
            int from = Long.numberOfTrailingZeros(bishops);
            long bishopMoves = bishopAttacks(from, bitboard.getOccupied()) & notOwnPieces;
            while (bishopMoves != 0) {
                int to = Long.numberOfTrailingZeros(bishopMoves);
                moves.add(new Move(from, to));
                bishopMoves &= bishopMoves - 1;
            }
            bishops &= bishops - 1;
        }
    }

    private void generateRookMoves(List<Move> moves, int pieceIndex) {
        long rooks = bitboard.getPieceBitboard(pieceIndex); // White rooks: 0x81 (a1:0, h1:7)
        long notOwnPieces = ~(pieceIndex < 6 ? bitboard.getWhitePieces() : bitboard.getBlackPieces());
        long occupied = bitboard.getOccupied();

        while (rooks != 0) {
            int from = Long.numberOfTrailingZeros(rooks);
            long rookMoves = rookAttacks(from, occupied) & notOwnPieces;
            while (rookMoves != 0) {
                int to = Long.numberOfTrailingZeros(rookMoves);
                moves.add(new Move(from, to));
                rookMoves &= rookMoves - 1;
            }
            rooks &= rooks - 1;
        }
    }

    private void generateQueenMoves(List<Move> moves, int pieceIndex) {
        long queens = bitboard.getPieceBitboard(pieceIndex);
        long notOwnPieces = ~(pieceIndex < 6 ? bitboard.getWhitePieces() : bitboard.getBlackPieces());
        long occupied = bitboard.getOccupied();

        while (queens != 0) {
            int from = Long.numberOfTrailingZeros(queens);
            long queenMoves = (rookAttacks(from, occupied) | bishopAttacks(from, occupied)) & notOwnPieces;
            while (queenMoves != 0) {
                int to = Long.numberOfTrailingZeros(queenMoves);
                moves.add(new Move(from, to));
                queenMoves &= queenMoves - 1;
            }
            queens &= queens - 1;
        }
    }

    private void generateKingMoves(List<Move> moves, int pieceIndex) {
        long king = bitboard.getPieceBitboard(pieceIndex);
        long notOwnPieces = ~(pieceIndex < 6 ? bitboard.getWhitePieces() : bitboard.getBlackPieces());

        if (king != 0) {
            int from = Long.numberOfTrailingZeros(king);
            long kingMoves = kingAttacks(from) & notOwnPieces;
            while (kingMoves != 0) {
                int to = Long.numberOfTrailingZeros(kingMoves);
                moves.add(new Move(from, to));
                kingMoves &= kingMoves - 1;
            }
        }
    }

    private long knightAttacks(int square) {
        long k = 1L << square;
        long attacks = 0L;
        attacks |= (k << 17) & 0xFEFEFEFEFEFEFEFEL; // Up 2, right 1
        attacks |= (k << 15) & 0x7F7F7F7F7F7F7F7FL; // Up 2, left 1
        attacks |= (k << 10) & 0xFCFCFCFCFCFCFCFCL; // Up 1, right 2
        attacks |= (k << 6) & 0x3F3F3F3F3F3F3F3FL;  // Up 1, left 2
        attacks |= (k >>> 6) & 0xFCFCFCFCFCFCFCFCL; // Down 1, right 2
        attacks |= (k >>> 10) & 0x3F3F3F3F3F3F3F3FL; // Down 1, left 2
        attacks |= (k >>> 15) & 0xFEFEFEFEFEFEFEFEL; // Down 2, right 1
        attacks |= (k >>> 17) & 0x7F7F7F7F7F7F7F7FL; // Down 2, left 1
        return attacks;
    }

    private long kingAttacks(int square) {
        long k = 1L << square;
        long attacks = (k >>> 8) | (k << 8);
        attacks |= ((k >>> 1) & 0x7F7F7F7F7F7F7F7FL) | ((k << 1) & 0xFEFEFEFEFEFEFEFEL);
        attacks |= ((k >>> 9) & 0x7F7F7F7F7F7F7F7FL) | ((k >>> 7) & 0xFEFEFEFEFEFEFEFEL);
        attacks |= ((k << 7) & 0x7F7F7F7F7F7F7F7FL) | ((k << 9) & 0xFEFEFEFEFEFEFEFEL);
        return attacks;
    }

    private long bishopAttacks(int square, long occupied) {
        long attacks = 0L;
        int rank = square / 8, file = square % 8;
        for (int r = rank + 1, f = file + 1; r < 8 && f < 8; r++, f++) {
            attacks |= (1L << (r * 8 + f));
            if ((occupied & (1L << (r * 8 + f))) != 0) break;
        }
        for (int r = rank - 1, f = file - 1; r >= 0 && f >= 0; r--, f--) {
            attacks |= (1L << (r * 8 + f));
            if ((occupied & (1L << (r * 8 + f))) != 0) break;
        }
        for (int r = rank + 1, f = file - 1; r < 8 && f >= 0; r++, f--) {
            attacks |= (1L << (r * 8 + f));
            if ((occupied & (1L << (r * 8 + f))) != 0) break;
        }
        for (int r = rank - 1, f = file + 1; r >= 0 && f < 8; r--, f++) {
            attacks |= (1L << (r * 8 + f));
            if ((occupied & (1L << (r * 8 + f))) != 0) break;
        }
        return attacks;
    }

    private long rookAttacks(int square, long occupied) {
        long attacks = 0L;
        int rank = square / 8, file = square % 8;

        // Right
        for (int f = file + 1; f < 8; f++) {
            long pos = (1L << (rank * 8 + f));
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }

        // Left
        for (int f = file - 1; f >= 0; f--) {
            long pos = (1L << (rank * 8 + f));
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }

        // Up
        for (int r = rank + 1; r < 8; r++) {
            long pos = (1L << (r * 8 + file));
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }

        // Down
        for (int r = rank - 1; r >= 0; r--) {
            long pos = (1L << (r * 8 + file));
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }

        return attacks;
    }

    private void logValidMoves(List<Move> moves) {
        if (!logger.isLoggable(Level.INFO)) return;

        StringBuilder validMoves = new StringBuilder("Valid Moves (" + moves.size() + "):\n");
        for (Move move : moves) {
            validMoves.append(move.toString()).append("\n");
        }
        logger.info(validMoves.toString());

        char[][] board = new char[8][8];
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                board[rank][file] = '.';
            }
        }
        for (Move move : moves) {
            int toRank = 7 - (move.toSquare() / 8);
            int toFile = move.toSquare() % 8;
            board[toRank][toFile] = 'X';
        }

        StringBuilder boardStr = new StringBuilder("Valid Moves on Board:\n");
        for (int rank = 7; rank >= 0; rank--) {
            boardStr.append(rank + 1).append(" ");
            for (int file = 0; file < 8; file++) {
                boardStr.append(board[rank][file]).append(" ");
            }
            boardStr.append("\n");
        }
        boardStr.append("   a b c d e f g h\n");
        logger.info(boardStr.toString());
    }
}