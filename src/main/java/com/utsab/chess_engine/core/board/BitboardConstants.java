package com.utsab.chess_engine.core.board;

import com.utsab.chess_engine.core.utils.BoardUtils;

/**
 * A utility class defining constant bitboards for chessboard features, such as ranks, files, diagonals,
 * anti-diagonals, castling masks, and piece starting positions. These bitboards are used for move generation,
 * evaluation, and board state management in a UCI-compatible chess engine.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @see <a href="https://www.chessprogramming.org/Bitboards">Chess Programming Wiki - Bitboards</a>
 * @since 2025
 */
public class BitboardConstants {

    /**
     * Bitboard representing all 64 squares (all bits set).
     */
    public static final long ALL_SQUARES = ~0L;

    /**
     * Bitboard representing no squares (all bits unset).
     */
    public static final long NO_SQUARES = 0L;

    /**
     * Bitboard for rank 1 (squares a1-h1).
     */
    public static final long RANK_1 = 0b0000000000000000000000000000000000000000000000000000000011111111L;
    public static final long RANK_2 = BitOperations.shiftNorth(RANK_1);
    public static final long RANK_3 = BitOperations.shiftNorth(RANK_2);
    public static final long RANK_4 = BitOperations.shiftNorth(RANK_3);
    public static final long RANK_5 = BitOperations.shiftNorth(RANK_4);
    public static final long RANK_6 = BitOperations.shiftNorth(RANK_5);
    public static final long RANK_7 = BitOperations.shiftNorth(RANK_6);
    public static final long RANK_8 = BitOperations.shiftNorth(RANK_7);

    /**
     * Bitboard for file A (squares a1-a8).
     */
    public static final long FILE_A = 0b0000000100000001000000010000000100000001000000010000000100000001L;
    public static final long FILE_B = BitOperations.shiftEast(FILE_A);
    public static final long FILE_C = BitOperations.shiftEast(FILE_B);
    public static final long FILE_D = BitOperations.shiftEast(FILE_C);
    public static final long FILE_E = BitOperations.shiftEast(FILE_D);
    public static final long FILE_F = BitOperations.shiftEast(FILE_E);
    public static final long FILE_G = BitOperations.shiftEast(FILE_F);
    public static final long FILE_H = BitOperations.shiftEast(FILE_G);

    /**
     * Array of bitboards for files A-H.
     */
    public static final long[] FILE_MASKS = {FILE_A, FILE_B, FILE_C, FILE_D, FILE_E, FILE_F, FILE_G, FILE_H};

    /**
     * Bitboards for diagonals (a1-h8, etc.).
     */
    public static final long DIAGONAL_1 = 0x1L;
    public static final long DIAGONAL_2 = 0x102L;
    public static final long DIAGONAL_3 = 0x10204L;
    public static final long DIAGONAL_4 = 0x1020408L;
    public static final long DIAGONAL_5 = 0x102040810L;
    public static final long DIAGONAL_6 = 0x10204081020L;
    public static final long DIAGONAL_7 = 0x1020408102040L;
    public static final long DIAGONAL_8 = 0x102040810204080L;
    public static final long DIAGONAL_9 = 0x204081020408000L;
    public static final long DIAGONAL_10 = 0x408102040800000L;
    public static final long DIAGONAL_11 = 0x810204080000000L;
    public static final long DIAGONAL_12 = 0x1020408000000000L;
    public static final long DIAGONAL_13 = 0x2040800000000000L;
    public static final long DIAGONAL_14 = 0x4080000000000000L;
    public static final long DIAGONAL_15 = 0x8000000000000000L;


    /**
     * Bitboards for anti-diagonals (h1-a8, etc.).
     */
    public static final long ANTI_DIAGONAL_MASK_1 = 0x80L;
    public static final long ANTI_DIAGONAL_MASK_2 = 0x8040L;
    public static final long ANTI_DIAGONAL_MASK_3 = 0x804020L;
    public static final long ANTI_DIAGONAL_MASK_4 = 0x80402010L;
    public static final long ANTI_DIAGONAL_MASK_5 = 0x8040201008L;
    public static final long ANTI_DIAGONAL_MASK_6 = 0x804020100804L;
    public static final long ANTI_DIAGONAL_MASK_7 = 0x80402010080402L;
    public static final long ANTI_DIAGONAL_MASK_8 = 0x8040201008040201L;
    public static final long ANTI_DIAGONAL_MASK_9 = 0x4020100804020100L;
    public static final long ANTI_DIAGONAL_MASK_10 = 0x2010080402010000L;
    public static final long ANTI_DIAGONAL_MASK_11 = 0x1008040201000000L;
    public static final long ANTI_DIAGONAL_MASK_12 = 0x804020100000000L;
    public static final long ANTI_DIAGONAL_MASK_13 = 0x402010000000000L;
    public static final long ANTI_DIAGONAL_MASK_14 = 0x201000000000000L;
    public static final long ANTI_DIAGONAL_MASK_15 = 0x100000000000000L;

    /**
     * Bitboards for squares that must be unoccupied for legal castling.
     */
    public static final long WHITE_QUEENSIDE_CASTLE_TRAVEL_MASK = 0x000000000000000EL;
    public static final long WHITE_KINGSIDE_CASTLE_TRAVEL_MASK = 0x0000000000000060L;
    public static final long BLACK_QUEENSIDE_CASTLE_TRAVEL_MASK = WHITE_QUEENSIDE_CASTLE_TRAVEL_MASK << (7 * 8);
    public static final long BLACK_KINGSIDE_CASTLE_TRAVEL_MASK = WHITE_KINGSIDE_CASTLE_TRAVEL_MASK << (7 * 8);

    /**
     * Bitboards for squares that must not be attacked for legal castling.
     */
    public static final long WHITE_QUEENSIDE_CASTLE_SAFE_MASK = 0x000000000000001CL;
    public static final long WHITE_KINGSIDE_CASTLE_SAFE_MASK = WHITE_QUEENSIDE_CASTLE_SAFE_MASK << 2;
    public static final long BLACK_QUEENSIDE_CASTLE_SAFE_MASK = WHITE_QUEENSIDE_CASTLE_SAFE_MASK << (7 * 8);
    public static final long BLACK_KINGSIDE_CASTLE_SAFE_MASK = WHITE_KINGSIDE_CASTLE_SAFE_MASK << (7 * 8);

    /**
     * Starting position bitboards for White pieces.
     */
    public static final long WHITE_PAWNS_START = 0b0000000000000000000000000000000000000000000000001111111100000000L;
    public static final long WHITE_KNIGHTS_START = 0b0000000000000000000000000000000000000000000000000000000001000010L;
    public static final long WHITE_BISHOPS_START = 0b0000000000000000000000000000000000000000000000000000000000100100L;
    public static final long WHITE_ROOKS_START = 0b0000000000000000000000000000000000000000000000000000000010000001L;
    public static final long WHITE_QUEENS_START = 0b0000000000000000000000000000000000000000000000000000000000001000L;
    public static final long WHITE_KING_START = 0b0000000000000000000000000000000000000000000000000000000000010000L;

    /**
     * Starting position bitboards for Black pieces.
     */
    public static final long BLACK_PAWNS_START = 0b0000000011111111000000000000000000000000000000000000000000000000L;
    public static final long BLACK_KNIGHTS_START = 0b0100001000000000000000000000000000000000000000000000000000000000L;
    public static final long BLACK_BISHOPS_START = 0b0010010000000000000000000000000000000000000000000000000000000000L;
    public static final long BLACK_ROOKS_START = 0b1000000100000000000000000000000000000000000000000000000000000000L;
    public static final long BLACK_QUEENS_START = 0b0000100000000000000000000000000000000000000000000000000000000000L;
    public static final long BLACK_KING_START = 0b0001000000000000000000000000000000000000000000000000000000000000L;

    /**
     * Initial castling rights bitmask (all castling options available).
     */
    public static final int INITIAL_CASTLING_RIGHTS = 0b1111;

    /**
     * Bitmasks for clearing castling rights.
     */
    public static final int CLEAR_WHITE_CASTLING_MASK = 0b1100;
    public static final int CLEAR_BLACK_CASTLING_MASK = 0b0011;
    public static final int CLEAR_WHITE_KINGSIDE_MASK = 0b1110;
    public static final int CLEAR_BLACK_KINGSIDE_MASK = 0b1011;
    public static final int CLEAR_WHITE_QUEENSIDE_MASK = 0b1101;
    public static final int CLEAR_BLACK_QUEENSIDE_MASK = 0b0111;

    /**
     * Bitboards for west adjacent files for each file.
     */
    public static final long[] WEST_FILE_MASK = generateWestFileMask();

    /**
     * Bitboards for east adjacent files for each file.
     */
    public static final long[] EAST_FILE_MASK = generateEastFileMask();

    /**
     * Bitboards for adjacent files (left and right) for each file.
     */
    public static final long[] ADJACENT_FILE_MASK = generateAdjacentFileMask();

    /**
     * Bitboards for triple files (current file and both adjacent files).
     */
    public static final long[] TRIPLE_FILE_MASK = generateTripleFileMask();

    /**
     * Bitboards for passed pawn masks (files ahead of a pawn that must be free of opponent pawns).
     */
    public static final long[] WHITE_PASSED_PAWN_MASK = generatePassedPawnMask(true);
    public static final long[] BLACK_PASSED_PAWN_MASK = generatePassedPawnMask(false);

    /**
     * Bitboards for pawn protection masks (squares protecting a pawn).
     */
    public static final long[] WHITE_PROTECTED_PAWN_MASK = generateProtectedPawnMask(true);
    public static final long[] BLACK_PROTECTED_PAWN_MASK = generateProtectedPawnMask(false);

    /**
     * Generates bitboards for west adjacent files.
     *
     * @return an array of bitboards for west adjacent files
     * @author Utsab Dahal
     */
    private static long[] generateWestFileMask() {
        long[] westFileMasks = new long[8];
        for (int i = 0; i < 8; i++) {
            westFileMasks[i] = i > 0 ? BitboardConstants.FILE_A << (i - 1) : 0;
        }
        return westFileMasks;
    }

    /**
     * Generates bitboards for east adjacent files.
     *
     * @return an array of bitboards for east adjacent files
     * @author Utsab Dahal
     */
    private static long[] generateEastFileMask() {
        long[] westFileMasks = new long[8];
        for (int i = 0; i < 8; i++) {
            westFileMasks[i] = i < 7 ? BitboardConstants.FILE_A << (i + 1) : 0;
        }
        return westFileMasks;
    }

    /**
     * Generates bitboards for adjacent files (left and right).
     *
     * @return an array of bitboards for adjacent files
     * @author Utsab Dahal
     */
    private static long[] generateAdjacentFileMask() {
        long[] adjacentFileMasks = new long[8];
        for (int i = 0; i < 8; i++) {
            long left = i > 0 ? BitboardConstants.FILE_A << (i - 1) : 0;
            long right = i < 7 ? BitboardConstants.FILE_A << (i + 1) : 0;
            adjacentFileMasks[i] = left | right;
        }
        return adjacentFileMasks;
    }

    /**
     * Generates bitboards for triple files (current file and both adjacent files).
     *
     * @return an array of bitboards for triple files
     * @author Utsab Dahal
     */
    private static long[] generateTripleFileMask() {
        long[] tripleFileMasks = new long[8];
        for (int i = 0; i < 8; i++) {
            long fileMask = BitboardConstants.FILE_MASKS[i];
            long adjacentFileMask = ADJACENT_FILE_MASK[i];
            tripleFileMasks[i] = fileMask | adjacentFileMask;
        }
        return tripleFileMasks;
    }

    /**
     * Generates bitboards for passed pawn masks, indicating files ahead of a pawn that must be free.
     *
     * @param isWhite true for White pawns, false for Black
     * @return an array of bitboards for passed pawn masks
     * @author Utsab Dahal
     */
    private static long[] generatePassedPawnMask(boolean isWhite) {
        long[] passedPawnMask = new long[64];
        for (int square = 0; square < 64; square++) {
            int file = BoardUtils.getFile(square);
            int rank = BoardUtils.getRank(square);

            long fileMask = BitboardConstants.FILE_MASKS[file];
            long tripleFileMask = fileMask | ADJACENT_FILE_MASK[file];

            long forwardMask = isWhite ? ~(Long.MAX_VALUE >>> (64 - 8 * (rank + 1))) : ((1L << 8 * rank) - 1);
            passedPawnMask[square] = tripleFileMask & forwardMask;
        }
        return passedPawnMask;
    }

    /**
     * Generates bitboards for pawn protection masks (squares protecting a pawn).
     *
     * @param isWhite true for White pawns, false for Black
     * @return an array of bitboards for pawn protection masks
     * @author Utsab Dahal
     */
    private static long[] generateProtectedPawnMask(boolean isWhite) {
        long[] pawnProtectionMask = new long[64];
        for (int square = 0; square < 64; square++) {
            long squareBB = 1L << square;
            pawnProtectionMask[square] = isWhite ?
                    BitOperations.shiftSouthEast(squareBB) | BitOperations.shiftSouthWest(squareBB) :
                    BitOperations.shiftNorthEast(squareBB) | BitOperations.shiftNorthWest(squareBB);
        }
        return pawnProtectionMask;
    }


}