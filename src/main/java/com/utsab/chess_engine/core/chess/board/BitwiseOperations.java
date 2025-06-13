package com.utsab.chess_engine.core.chess.board;

import com.utsab.chess_engine.core.chess.utils.BoardUtils;

/**
 * A utility class providing bitwise operations for manipulating 64-bit bitboards in a chess engine.
 * Bitboards represent chessboard states (e.g., piece positions, ranks, files) as 64-bit integers, where
 * each bit corresponds to a square (0-63). This class supports operations like shifting, bit counting,
 * and pawn move calculations, optimized for performance in UCI-compatible chess engines.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Bitboards">Chess Programming Wiki - Bitboards</a>
 */
public class BitwiseOperations {

    /**
     * Retrieves the index of the least-significant set bit (LSB) in a bitboard.
     * Used for iterating through set bits, typically to process occupied squares.
     *
     * @param board the bitboard to analyze
     * @return the index (0-63) of the least-significant set bit, or 64 if the bitboard is empty
     * @author Utsab Dahal
     */
    public static int getNextBit(long board) {
        return Long.numberOfTrailingZeros(board);
    }

    /**
     * Removes the least-significant set bit from a bitboard and returns the result.
     * Used in conjunction with {@link #getNextBit(long)} for iterating through set bits.
     *
     * @param board the bitboard to modify
     * @return the bitboard with its least-significant set bit cleared
     * @author Utsab Dahal
     */
    public static long popBit(long board) {
        return board & (board - 1);
    }

    /**
     * Counts the number of set bits (1s) in a bitboard.
     * Useful for determining the number of pieces or occupied squares.
     *
     * @param board the bitboard to analyze
     * @return the number of set bits
     * @author Utsab Dahal
     */
    public static int countBits(long board) {
        return Long.bitCount(board);
    }

    /**
     * Shifts a bitboard north (up one rank, i.e., 8 squares).
     *
     * @param board the bitboard to shift
     * @return the shifted bitboard
     * @author Utsab Dahal
     */
    public static long shiftNorth(long board) {
        return board << 8;
    }

    /**
     * Shifts a bitboard south (down one rank, i.e., 8 squares).
     *
     * @param board the bitboard to shift
     * @return the shifted bitboard
     * @author Utsab Dahal
     */
    public static long shiftSouth(long board) {
        return board >>> 8;
    }

    /**
     * Shifts a bitboard east (right one file), masking off bits that wrap to file A.
     *
     * @param board the bitboard to shift
     * @return the shifted bitboard
     * @author Utsab Dahal
     */
    public static long shiftEast(long board) {
        return (board << 1) & ~Bits.FILE_A;
    }

    /**
     * Shifts a bitboard west (left one file), masking off bits that wrap to file H.
     *
     * @param board the bitboard to shift
     * @return the shifted bitboard
     * @author Utsab Dahal
     */
    public static long shiftWest(long board) {
        return (board >>> 1) & ~Bits.FILE_H;
    }

    /**
     * Shifts a bitboard north-east (up one rank, right one file), masking off file A.
     *
     * @param board the bitboard to shift
     * @return the shifted bitboard
     * @author Utsab Dahal
     */
    public static long shiftNorthEast(long board) {
        return (board << 9) & ~Bits.FILE_A;
    }

    /**
     * Shifts a bitboard south-east (down one rank, right one file), masking off file A.
     *
     * @param board the bitboard to shift
     * @return the shifted bitboard
     * @author Utsab Dahal
     */
    public static long shiftSouthEast(long board) {
        return (board >>> 7) & ~Bits.FILE_A;
    }

    /**
     * Shifts a bitboard north-west (up one rank, left one file), masking off file H.
     *
     * @param board the bitboard to shift
     * @return the shifted bitboard
     * @author Utsab Dahal
     */
    public static long shiftNorthWest(long board) {
        return (board << 7) & ~Bits.FILE_H;
    }

    /**
     * Shifts a bitboard south-west (down one rank, left one file), masking off file H.
     *
     * @param board the bitboard to shift
     * @return the shifted bitboard
     * @author Utsab Dahal
     */
    public static long shiftSouthWest(long board) {
        return (board >>> 9) & ~Bits.FILE_H;
    }

    /**
     * Calculates single pawn pushes (one square forward) for the given pawn bitboard.
     *
     * @param pawns the bitboard representing pawn positions
     * @param occupied the bitboard of all occupied squares
     * @param isWhite true for White pawns, false for Black
     * @return a bitboard of valid single pawn push destinations
     * @author Utsab Dahal
     */
    public static long pawnSingleMoves(long pawns, long occupied, boolean isWhite) {
        return isWhite ?
                shiftNorth(pawns) & ~occupied & ~Bits.RANK_8 :
                shiftSouth(pawns) & ~occupied & ~Bits.RANK_1;
    }

    /**
     * Calculates double pawn pushes (two squares forward) from the second rank (White) or seventh rank (Black).
     *
     * @param pawns the bitboard representing pawn positions
     * @param occupied the bitboard of all occupied squares
     * @param isWhite true for White pawns, false for Black
     * @return a bitboard of valid double pawn push destinations
     * @author Utsab Dahal
     */
    public static long pawnDoubleMoves(long pawns, long occupied, boolean isWhite) {
        return isWhite ?
                shiftNorth(pawnSingleMoves(pawns, occupied, true)) & ~occupied & Bits.RANK_4 :
                shiftSouth(pawnSingleMoves(pawns, occupied, false)) & ~occupied & Bits.RANK_5;
    }

    /**
     * Calculates pawn pushes that result in promotion (reaching the 8th rank for White or 1st rank for Black).
     *
     * @param pawns the bitboard representing pawn positions
     * @param occupied the bitboard of all occupied squares
     * @param isWhite true for White pawns, false for Black
     * @return a bitboard of pawn push promotion destinations
     * @author Utsab Dahal
     */
    public static long pawnPushPromotions(long pawns, long occupied, boolean isWhite) {
        return isWhite ?
                shiftNorth(pawns) & ~occupied & Bits.RANK_8 :
                shiftSouth(pawns) & ~occupied & Bits.RANK_1;
    }

    /**
     * Calculates left captures (north-west for White, south-west for Black) by pawns.
     *
     * @param pawns the bitboard representing pawn positions
     * @param opponents the bitboard of opponent pieces
     * @param isWhite true for White pawns, false for Black
     * @return a bitboard of valid left capture destinations
     * @author Utsab Dahal
     */
    public static long pawnLeftCaptures(long pawns, long opponents, boolean isWhite) {
        return isWhite ?
                shiftNorthWest(pawns) & opponents & ~Bits.FILE_H & ~Bits.RANK_8 :
                shiftSouthWest(pawns) & opponents & ~Bits.FILE_H & ~Bits.RANK_1;
    }

    /**
     * Calculates right captures (north-east for White, south-east for Black) by pawns.
     *
     * @param pawns the bitboard representing pawn positions
     * @param opponents the bitboard of opponent pieces
     * @param isWhite true for White pawns, false for Black
     * @return a bitboard of valid right capture destinations
     * @author Utsab Dahal
     */
    public static long pawnRightCaptures(long pawns, long opponents, boolean isWhite) {
        return isWhite ?
                shiftNorthEast(pawns) & opponents & ~Bits.FILE_A & ~Bits.RANK_8 :
                shiftSouthEast(pawns) & opponents & ~Bits.FILE_A & ~Bits.RANK_1;
    }

    /**
     * Calculates left en passant captures by pawns.
     *
     * @param pawns the bitboard representing pawn positions
     * @param enPassantFile the bitboard of the en passant target file
     * @param isWhite true for White pawns, false for Black
     * @return a bitboard of valid left en passant capture destinations
     * @author Utsab Dahal
     */
    public static long pawnLeftEnPassants(long pawns, long enPassantFile, boolean isWhite) {
        return isWhite ?
                shiftNorthWest(pawns) & enPassantFile & Bits.RANK_6 & ~Bits.FILE_H :
                shiftSouthWest(pawns) & enPassantFile & Bits.RANK_3 & ~Bits.FILE_H;
    }

    /**
     * Calculates right en passant captures by pawns.
     *
     * @param pawns the bitboard representing pawn positions
     * @param enPassantFile the bitboard of the en passant target file
     * @param isWhite true for White pawns, false for Black
     * @return a bitboard of valid right en passant capture destinations
     * @author Utsab Dahal
     */
    public static long pawnRightEnPassants(long pawns, long enPassantFile, boolean isWhite) {
        return isWhite ?
                shiftNorthEast(pawns) & enPassantFile & Bits.RANK_6 & ~Bits.FILE_A :
                shiftSouthEast(pawns) & enPassantFile & Bits.RANK_3 & ~Bits.FILE_A;
    }

    /**
     * Calculates left capture promotions by pawns (north-west for White, south-west for Black).
     *
     * @param pawns the bitboard representing pawn positions
     * @param opponents the bitboard of opponent pieces
     * @param isWhite true for White pawns, false for Black
     * @return a bitboard of valid left capture promotion destinations
     * @author Utsab Dahal
     */
    public static long pawnLeftCapturePromotions(long pawns, long opponents, boolean isWhite) {
        return isWhite ?
                shiftNorthWest(pawns) & opponents & ~Bits.FILE_H & Bits.RANK_8 :
                shiftSouthWest(pawns) & opponents & ~Bits.FILE_H & Bits.RANK_1;
    }

    /**
     * Calculates right capture promotions by pawns (north-east for White, south-east for Black).
     *
     * @param pawns the bitboard representing pawn positions
     * @param opponents the bitboard of opponent pieces
     * @param isWhite true for White pawns, false for Black
     * @return a bitboard of valid right capture promotion destinations
     * @author Utsab Dahal
     */
    public static long pawnRightCapturePromotions(long pawns, long opponents, boolean isWhite) {
        return isWhite ?
                shiftNorthEast(pawns) & opponents & ~Bits.FILE_A & Bits.RANK_8 :
                shiftSouthEast(pawns) & opponents & ~Bits.FILE_A & Bits.RANK_1;
    }

    /**
     * Determines if a pawn is passed (no opponent pawns can block or capture it).
     *
     * @param pawn the square index (0-63) of the pawn
     * @param opponentPawns the bitboard of opponent pawns
     * @param isWhite true for White pawn, false for Black
     * @return true if the pawn is passed, false otherwise
     * @author Utsab Dahal
     */
    public static boolean isPassedPawn(int pawn, long opponentPawns, boolean isWhite) {
        long passedPawnMask = isWhite ? Bits.WHITE_PASSED_PAWN_MASK[pawn] : Bits.BLACK_PASSED_PAWN_MASK[pawn];
        return (passedPawnMask & opponentPawns) == 0;
    }

    /**
     * Determines if a pawn is isolated (no friendly pawns on adjacent files).
     *
     * @param file the file index (0-7) of the pawn
     * @param friendlyPawns the bitboard of friendly pawns
     * @return true if the pawn is isolated, false otherwise
     * @author Utsab Dahal
     */
    public static boolean isIsolatedPawn(int file, long friendlyPawns) {
        return (Bits.ADJACENT_FILE_MASK[file] & friendlyPawns) == 0;
    }

    /**
     * Determines if a file contains doubled pawns (multiple friendly pawns on the same file).
     *
     * @param file the file index (0-7) to check
     * @param friendlyPawns the bitboard of friendly pawns
     * @return true if the file contains doubled pawns, false otherwise
     * @author Utsab Dahal
     */
    public static boolean isDoubledPawn(int file, long friendlyPawns) {
        long fileMask = Bits.FILE_MASKS[file];
        return countBits(friendlyPawns & fileMask) > 1;
    }

    /**
     * Retrieves the pawn shield bitboard for a king, covering the three files in front of it.
     * @param kingFile the file index (0-7) of the king
     * @param pawns the bitboard of friendly pawns
     * @return a bitboard representing the pawn shield
     * @author Utsab Dahal
     */
    public static long getPawnShield(int kingFile, long pawns) {
        long tripleFileMask = Bits.TRIPLE_FILE_MASK[kingFile];
        return tripleFileMask & pawns;
    }

    /**
     * Counts the number of friendly pawns protecting a given pawn.
     *
     * @param pawn the square index (0-63) of the pawn
     * @param friendlyPawns the bitboard of friendly pawns
     * @param isWhite true for White pawn, false for Black
     * @return the number of protecting pawns
     * @author Utsab Dahal
     */
    public static int countPawnProtectors(int pawn, long friendlyPawns, boolean isWhite) {
        long protectionMask = isWhite ? Bits.WHITE_PROTECTED_PAWN_MASK[pawn] : Bits.BLACK_PROTECTED_PAWN_MASK[pawn];
        return countBits(protectionMask & friendlyPawns);
    }


    /**
     * Prints a bitboard in a human-readable 8x8 grid format, with 1s for set bits and 0s for unset bits.
     *
     * @param board the bitboard to print
     */
    public static void print(long board) {
        String s = Long.toBinaryString(board);
        for (int i = 7; i >= 0; i--) {
            for (int n = 0; n < 8; n++) {
                int index = BoardUtils.squareIndex(i, n);
                boolean isBit = ((board >>> index) & 1) == 1;
                System.out.print(isBit ? 1 : 0);
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Retrieves the bitboard for a specific file.
     *
     * @param file the file index (-1 for none, 0-7 for files a-h)
     * @return the bitboard for the specified file, or 0 if file is -1
     * @throws IllegalArgumentException if the file index is invalid
     * @author Utsab Dahal
     */
    public static long getFileBitboard(int file) {
        return switch (file) {
            case -1 -> 0L;
            case 0 -> Bits.FILE_A;
            case 1 -> Bits.FILE_B;
            case 2 -> Bits.FILE_C;
            case 3 -> Bits.FILE_D;
            case 4 -> Bits.FILE_E;
            case 5 -> Bits.FILE_F;
            case 6 -> Bits.FILE_G;
            case 7 -> Bits.FILE_H;
            default -> throw new IllegalArgumentException("Invalid file: " + file);
        };
    }
}