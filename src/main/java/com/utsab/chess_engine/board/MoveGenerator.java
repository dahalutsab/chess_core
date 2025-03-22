package com.utsab.chess_engine.board;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generates all legal moves for a given chess position represented by a Bitboard.
 * Supports all chess rules including pawn promotion, en passant, and castling.
 */
public class MoveGenerator {
    private static final Logger logger = Logger.getLogger(MoveGenerator.class.getName());
    private final Bitboard bitboard;

    // Precomputed attack tables (order matters for static initialization)
    protected static final long[] FILE_MASKS;
    protected static final long[] KNIGHT_ATTACKS;
    protected static final long[] KING_ATTACKS;

    // Static initialization block to ensure correct order
    static {
        FILE_MASKS = precomputeFileMasks();
        KNIGHT_ATTACKS = precomputeKnightAttacks();
        KING_ATTACKS = precomputeKingAttacks();
    }

    // Unused fields removed: ROOK_MASKS, BISHOP_MASKS, RANK_MASKS
    // If needed later (e.g., for magic bitboards), they can be re-added

    public MoveGenerator(Bitboard bitboard) {
        this.bitboard = bitboard;
    }

    public List<Move> generateMoves() {
        List<Move> moves = new ArrayList<>();
        boolean white = bitboard.isWhiteToMove();
        int baseIndex = white ? 0 : 6;

        generatePawnMoves(moves, white);
        generateKnightMoves(moves, baseIndex + 1);
        generateBishopMoves(moves, baseIndex + 2);
        generateRookMoves(moves, baseIndex + 3);
        generateQueenMoves(moves, baseIndex + 4);
        generateKingMoves(moves, baseIndex + 5, white);

        List<Move> legalMoves = new ArrayList<>();
        for (Move move : moves) {
            Bitboard.MoveInfo info = bitboard.makeMove(move);
            if (!bitboard.isInCheck(white)) {
                legalMoves.add(move);
            }
            bitboard.undoMove(info);
        }

        logger.info("Generated " + legalMoves.size() + " legal moves for " + (white ? "White" : "Black"));
        return legalMoves;
    }


    private void generatePawnMoves(List<Move> moves, boolean white) {
        long pawns = bitboard.getPieceBitboard(white ? 0 : 6);
        if (pawns == 0) return; // No pawns, no moves
        long empty = ~bitboard.getOccupied();
        long targets = white ? bitboard.getBlackPieces() : bitboard.getWhitePieces();
        int direction = white ? 8 : -8;
        int epSquare = bitboard.getEnPassantSquare();

        // Single push
        long singlePush = white ? (pawns << 8) : (pawns >>> 8);
        long validSingle = singlePush & empty;
        while (validSingle != 0) {
            int to = Long.numberOfTrailingZeros(validSingle);
            int from = to - direction;
            addPawnMoves(moves, from, to, white);
            validSingle &= validSingle - 1;
        }

        // Double push
        long doublePush = white ? ((pawns & 0xFF00L) << 16) : ((pawns & 0xFF000000000000L) >>> 16);
        long validDouble = doublePush & empty & (white ? (empty << 8) : (empty >>> 8));
        while (validDouble != 0) {
            int to = Long.numberOfTrailingZeros(validDouble);
            int from = to - 2 * direction;
            moves.add(new Move(from, to));
            validDouble &= validDouble - 1;
        }

        // Captures
        long leftCapture = white ? ((pawns & ~FILE_MASKS[0]) << 7) : ((pawns & ~FILE_MASKS[0]) >>> 9);
        long rightCapture = white ? ((pawns & ~FILE_MASKS[7]) << 9) : ((pawns & ~FILE_MASKS[7]) >>> 7);
        long validLeft = leftCapture & (targets | (epSquare >= 0 ? (1L << epSquare) : 0L));
        long validRight = rightCapture & (targets | (epSquare >= 0 ? (1L << epSquare) : 0L));

        while (validLeft != 0) {
            int to = Long.numberOfTrailingZeros(validLeft);
            int from = to - (white ? 7 : -9);
            addPawnMoves(moves, from, to, white);
            validLeft &= validLeft - 1;
        }
        while (validRight != 0) {
            int to = Long.numberOfTrailingZeros(validRight);
            int from = to - (white ? 9 : -7);
            addPawnMoves(moves, from, to, white);
            validRight &= validRight - 1;
        }
    }

    private void addPawnMoves(List<Move> moves, int from, int to, boolean white) {
        if ((white && to >= 56) || (!white && to <= 7)) {
            moves.add(new Move(from, to)); // Promotion handled in Bitboard.makeMove
        } else {
            moves.add(new Move(from, to));
        }
    }

    private void generateKnightMoves(List<Move> moves, int pieceIndex) {
        long knights = bitboard.getPieceBitboard(pieceIndex);
        long notOwnPieces = ~(pieceIndex < 6 ? bitboard.getWhitePieces() : bitboard.getBlackPieces());

        while (knights != 0) {
            int from = Long.numberOfTrailingZeros(knights);
            long attacks = KNIGHT_ATTACKS[from] & notOwnPieces;
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                moves.add(new Move(from, to));
                attacks &= attacks - 1;
            }
            knights &= knights - 1;
        }
    }

    private void generateBishopMoves(List<Move> moves, int pieceIndex) {
        long bishops = bitboard.getPieceBitboard(pieceIndex);
        long notOwnPieces = ~(pieceIndex < 6 ? bitboard.getWhitePieces() : bitboard.getBlackPieces());

        while (bishops != 0) {
            int from = Long.numberOfTrailingZeros(bishops);
            long attacks = getBishopAttacks(from, bitboard.getOccupied()) & notOwnPieces;
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                moves.add(new Move(from, to));
                attacks &= attacks - 1;
            }
            bishops &= bishops - 1;
        }
    }

    private void generateRookMoves(List<Move> moves, int pieceIndex) {
        long rooks = bitboard.getPieceBitboard(pieceIndex);
        long notOwnPieces = ~(pieceIndex < 6 ? bitboard.getWhitePieces() : bitboard.getBlackPieces());

        while (rooks != 0) {
            int from = Long.numberOfTrailingZeros(rooks);
            long attacks = getRookAttacks(from, bitboard.getOccupied()) & notOwnPieces;
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                moves.add(new Move(from, to));
                attacks &= attacks - 1;
            }
            rooks &= rooks - 1;
        }
    }

    private void generateQueenMoves(List<Move> moves, int pieceIndex) {
        long queens = bitboard.getPieceBitboard(pieceIndex);
        long notOwnPieces = ~(pieceIndex < 6 ? bitboard.getWhitePieces() : bitboard.getBlackPieces());

        while (queens != 0) {
            int from = Long.numberOfTrailingZeros(queens);
            long attacks = (getRookAttacks(from, bitboard.getOccupied()) |
                    getBishopAttacks(from, bitboard.getOccupied())) & notOwnPieces;
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                moves.add(new Move(from, to));
                attacks &= attacks - 1;
            }
            queens &= queens - 1;
        }
    }

    private void generateKingMoves(List<Move> moves, int pieceIndex, boolean white) {
        long king = bitboard.getPieceBitboard(pieceIndex);
        long notOwnPieces = ~(white ? bitboard.getWhitePieces() : bitboard.getBlackPieces());
        int from = Long.numberOfTrailingZeros(king);

        long attacks = KING_ATTACKS[from] & notOwnPieces;
        while (attacks != 0) {
            int to = Long.numberOfTrailingZeros(attacks);
            moves.add(new Move(from, to));
            attacks &= attacks - 1;
        }

        int castlingRights = bitboard.getCastlingRights();
        long occupied = bitboard.getOccupied();
        if (white && from == 4 && !bitboard.isInCheck(true)) {
            if ((castlingRights & 0b0100) != 0 && (occupied & 0x60L) == 0 &&
                    !isSquareAttacked(5, true) && !isSquareAttacked(6, true)) {
                moves.add(new Move(4, 6));
            }
            if ((castlingRights & 0b1000) != 0 && (occupied & 0xEL) == 0 &&
                    !isSquareAttacked(3, true) && !isSquareAttacked(2, true)) {
                moves.add(new Move(4, 2));
            }
        } else if (!white && from == 60 && !bitboard.isInCheck(false)) {
            if ((castlingRights & 0b0001) != 0 && (occupied & 0x6000000000000000L) == 0 &&
                    !isSquareAttacked(61, false) && !isSquareAttacked(62, false)) {
                moves.add(new Move(60, 62));
            }
            if ((castlingRights & 0b0010) != 0 && (occupied & 0xE00000000000000L) == 0 &&
                    !isSquareAttacked(59, false) && !isSquareAttacked(58, false)) {
                moves.add(new Move(60, 58));
            }
        }
    }

    private boolean isSquareAttacked(int square, boolean white) {
        long opponents = white ? bitboard.getBlackPieces() : bitboard.getWhitePieces();
        int oppBase = white ? 6 : 0;

        if ((KNIGHT_ATTACKS[square] & bitboard.getPieceBitboard(oppBase + 1)) != 0) return true;
        if ((getBishopAttacks(square, bitboard.getOccupied()) &
                (bitboard.getPieceBitboard(oppBase + 2) | bitboard.getPieceBitboard(oppBase + 4))) != 0) return true;
        if ((getRookAttacks(square, bitboard.getOccupied()) &
                (bitboard.getPieceBitboard(oppBase + 3) | bitboard.getPieceBitboard(oppBase + 4))) != 0) return true;
        long pawnAttacks = white ?
                (((1L << (square - 9)) & ~FILE_MASKS[7]) | ((1L << (square - 7)) & ~FILE_MASKS[0])) :
                (((1L << (square + 9)) & ~FILE_MASKS[0]) | ((1L << (square + 7)) & ~FILE_MASKS[7]));
        return (pawnAttacks & bitboard.getPieceBitboard(oppBase)) != 0;
    }

    private static long[] precomputeKnightAttacks() {
        long[] attacks = new long[64];
        for (int square = 0; square < 64; square++) {
            long k = 1L << square;
            attacks[square] = ((k << 17) & ~FILE_MASKS[0]) | ((k << 15) & ~FILE_MASKS[7]) |
                    ((k << 10) & ~(FILE_MASKS[6] | FILE_MASKS[7])) | ((k << 6) & ~(FILE_MASKS[0] | FILE_MASKS[1])) |
                    ((k >>> 6) & ~(FILE_MASKS[6] | FILE_MASKS[7])) | ((k >>> 10) & ~(FILE_MASKS[0] | FILE_MASKS[1])) |
                    ((k >>> 15) & ~FILE_MASKS[0]) | ((k >>> 17) & ~FILE_MASKS[7]);
        }
        return attacks;
    }

    private static long[] precomputeKingAttacks() {
        long[] attacks = new long[64];
        for (int square = 0; square < 64; square++) {
            long k = 1L << square;
            attacks[square] = (k >>> 8) | (k << 8) |
                    ((k >>> 1) & ~FILE_MASKS[7]) | ((k << 1) & ~FILE_MASKS[0]) |
                    ((k >>> 9) & ~FILE_MASKS[7]) | ((k >>> 7) & ~FILE_MASKS[0]) |
                    ((k << 7) & ~FILE_MASKS[7]) | ((k << 9) & ~FILE_MASKS[0]);
        }
        return attacks;
    }

    private static long[] precomputeFileMasks() {
        long[] masks = new long[8];
        for (int file = 0; file < 8; file++) {
            masks[file] = 0x0101010101010101L << file;
        }
        return masks;
    }

    public static long getRookAttacks(int square, long occupied) {
        long attacks = 0L;
        int rank = square / 8;
        int file = square % 8;

        for (int f = file + 1; f < 8; f++) {
            long pos = 1L << (rank * 8 + f);
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }
        for (int f = file - 1; f >= 0; f--) {
            long pos = 1L << (rank * 8 + f);
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }
        for (int r = rank + 1; r < 8; r++) {
            long pos = 1L << (r * 8 + file);
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }
        for (int r = rank - 1; r >= 0; r--) {
            long pos = 1L << (r * 8 + file);
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }
        return attacks;
    }

    public static long getBishopAttacks(int square, long occupied) {
        long attacks = 0L;
        int rank = square / 8;
        int file = square % 8;

        for (int r = rank + 1, f = file + 1; r < 8 && f < 8; r++, f++) {
            long pos = 1L << (r * 8 + f);
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }
        for (int r = rank - 1, f = file - 1; r >= 0 && f >= 0; r--, f--) {
            long pos = 1L << (r * 8 + f);
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }
        for (int r = rank + 1, f = file - 1; r < 8 && f >= 0; r++, f--) {
            long pos = 1L << (r * 8 + f);
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }
        for (int r = rank - 1, f = file + 1; r >= 0 && f < 8; r--, f++) {
            long pos = 1L << (r * 8 + f);
            attacks |= pos;
            if ((occupied & pos) != 0) break;
        }
        return attacks;
    }
}