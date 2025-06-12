package com.utsab.chess_engine.core.chess.board;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.Random;

/**
 * A utility class for generating and updating Zobrist keys, 64-bit values that uniquely represent chess positions.
 * Zobrist hashing is used to detect repeated positions, optimize transposition tables, and reduce redundant
 * evaluations in a UCI-compatible chess engine.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Zobrist_Hashing">Chess Programming Wiki - Zobrist Hashing</a>
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Zobrist {

    /**
     * 3D array of Zobrist keys for pieces on squares [square][color][pieceType].
     */
    static final long[][][] PIECE_SQUARE_HASH = new long[64][2][6];

    /**
     * Zobrist keys for castling rights combinations (0-15).
     */
    static final long[] CASTLING_RIGHTS = new long[16];

    /**
     * Zobrist keys for en passant files (-1 to 7, indexed 0-8).
     */
    static final long[] EN_PASSANT_FILE = new long[9];

    /**
     * Zobrist key for Black to move.
     */
    static final long BLACK_TO_MOVE;

    /**
     * Index for White pieces in PIECE_SQUARE_HASH.
     */
    static final int WHITE = 0;

    /**
     * Index for Black pieces in PIECE_SQUARE_HASH.
     */
    static final int BLACK = 1;

    /*
      Initializes Zobrist keys with random 64-bit values.
     */
    static {
        Random random = new Random();
        for (int square = 0; square < 64; square++) {
            for (int pieceIndex : Arrays.stream(Piece.values()).map(Piece::getIndex).toList()) {
                PIECE_SQUARE_HASH[square][WHITE][pieceIndex] = random.nextLong();
                PIECE_SQUARE_HASH[square][BLACK][pieceIndex] = random.nextLong();
            }
        }
        for (int i = 0; i < CASTLING_RIGHTS.length; i++) {
            CASTLING_RIGHTS[i] = random.nextLong();
        }
        for (int i = 0; i < EN_PASSANT_FILE.length; i++) {
            EN_PASSANT_FILE[i] = random.nextLong();
        }
        BLACK_TO_MOVE = random.nextLong();
    }

    /**
     * Generates a Zobrist key for the entire board position, including pieces, side to move, castling rights,
     * and en passant file.
     *
     * @param board the board to hash
     * @return the Zobrist key
     */
    public static long generateKey(Board board) {
        long key = 0L;

        for (int square = 0; square < 64; square++) {
            if (((board.getWhitePawns() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][WHITE][Piece.PAWN.getIndex()];
            } else if (((board.getBlackPawns() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][BLACK][Piece.PAWN.getIndex()];
            } else if (((board.getWhiteKnights() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][WHITE][Piece.KNIGHT.getIndex()];
            } else if (((board.getBlackKnights() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][BLACK][Piece.KNIGHT.getIndex()];
            } else if (((board.getWhiteBishops() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][WHITE][Piece.BISHOP.getIndex()];
            } else if (((board.getBlackBishops() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][BLACK][Piece.BISHOP.getIndex()];
            } else if (((board.getWhiteRooks() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][WHITE][Piece.ROOK.getIndex()];
            } else if (((board.getBlackRooks() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][BLACK][Piece.ROOK.getIndex()];
            } else if (((board.getWhiteQueens() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][WHITE][Piece.QUEEN.getIndex()];
            } else if (((board.getBlackQueens() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][BLACK][Piece.QUEEN.getIndex()];
            } else if (((board.getWhiteKing() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][WHITE][Piece.KING.getIndex()];
            } else if (((board.getBlackKing() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][BLACK][Piece.KING.getIndex()];
            }
        }

        int enPassantFile = board.getGameState().getEnPassantFile() + 1;
        key ^= EN_PASSANT_FILE[enPassantFile];

        if (!board.isWhiteToMove()) {
            key ^= BLACK_TO_MOVE;
        }

        key ^= CASTLING_RIGHTS[board.getGameState().getCastlingRights()];

        return key;
    }

    /**
     * Generates a Zobrist key for the pawn structure only, used for pawn-specific evaluations.
     *
     * @param board the board to hash
     * @return the pawn Zobrist key
     */
    public static long generatePawnKey(Board board) {
        long key = 0L;
        for (int square = 0; square < 64; square++) {
            if (((board.getWhitePawns() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][WHITE][Piece.PAWN.getIndex()];
            } else if (((board.getBlackPawns() >>> square) & 1) == 1) {
                key ^= PIECE_SQUARE_HASH[square][BLACK][Piece.PAWN.getIndex()];
            }
        }
        return key;

    }

    /**
     * Updates a Zobrist key by toggling a piece on a square.
     *
     * @param key the current Zobrist key
     * @param square the square index (0-63)
     * @param pieceType the piece type
     * @param isWhite true for White, false for Black
     * @return the updated Zobrist key
     */
    public static long updatePiece(long key, int square, Piece pieceType, boolean isWhite) {
        return key ^ PIECE_SQUARE_HASH[square][isWhite ? WHITE : BLACK][pieceType.getIndex()];
    }

    /**
     * Updates a Zobrist key with new castling rights.
     *
     * @param key the current Zobrist key
     * @param oldCastlingRights the previous castling rights mask
     * @param newCastlingRights the new castling rights mask
     * @return the updated Zobrist key
     */
    public static long updateCastlingRights(long key, int oldCastlingRights, int newCastlingRights) {
        key ^= CASTLING_RIGHTS[oldCastlingRights];
        key ^= CASTLING_RIGHTS[newCastlingRights];
        return key;
    }

    /**
     * Updates a Zobrist key with a change in en passant file.
     *
     * @param key the current Zobrist key
     * @param oldEnPassantFile the previous en passant file (-1 to 7)
     * @param newEnPassantFile the new en passant file (-1 to 7)
     * @return the updated Zobrist key
     */
    public static long updateEnPassantFile(long key, int oldEnPassantFile, int newEnPassantFile) {
        key ^= EN_PASSANT_FILE[oldEnPassantFile + 1];
        key ^= EN_PASSANT_FILE[newEnPassantFile + 1];
        return key;
    }

    /**
     * Updates a Zobrist key by toggling the side to move.
     *
     * @param key the current Zobrist key
     * @return the updated Zobrist key
     */
    public static long updateSideToMove(long key) {
        return key ^ BLACK_TO_MOVE;
    }

    /**
     * Updates a Zobrist key after a null move, toggling the side to move and clearing en passant file.
     *
     * @param key the current Zobrist key
     * @param oldEnPassantFile the previous en passant file
     * @return the updated Zobrist key
     */
    public static long updateKeyAfterNullMove(long key, int oldEnPassantFile) {
        key ^= EN_PASSANT_FILE[oldEnPassantFile + 1];
        key ^= EN_PASSANT_FILE[0];
        return key ^ BLACK_TO_MOVE;
    }
}