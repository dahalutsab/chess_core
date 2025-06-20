package com.utsab.chess_engine.core.utils;

import com.utsab.chess_engine.core.board.*;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Set;

/**
 * Utility class providing static methods for chess board operations.
 * Supports piece movement calculations, board state manipulation, and coordinate conversions.
 * Used by the chess engine to manage board-related logic efficiently.
 *
 * @author Utsab Dahal
 */
public class BoardUtils {

    /**
     * Set of move vectors for sliding pieces along diagonals (e.g., bishops, queens).
     * Represents offsets for directions: up-left (-9), up-right (-7), down-left (7), down-right (9).
     */
    public static final Set<Integer> DIAGONAL_MOVE_VECTORS = Set.of(-9, -7, 7, 9);

    /**
     * Set of move vectors for sliding pieces along orthogonal lines (e.g., rooks, queens).
     * Represents offsets for directions: up (-8), left (-1), right (1), down (8).
     */
    public static final Set<Integer> ORTHOGONAL_MOVE_VECTORS = Set.of(-8, -1, 1, 8);

    /**
     * Set of offset exceptions for sliding pieces on the a-file (file 0).
     * Prevents illegal moves that would wrap around to the h-file.
     */
    public static final Set<Integer> A_FILE_OFFSET_EXCEPTIONS = Set.of(-9, -1, 7);

    /**
     * Set of offset exceptions for sliding pieces on the h-file (file 7).
     * Prevents illegal moves that would wrap around to the a-file.
     */
    public static final Set<Integer> H_FILE_OFFSET_EXCEPTIONS = Set.of(-7, 1, 9);

    /**
     * Returns the initial piece arrangement for a standard chess game.
     * The array represents a 64-square board in rank-major order (rank 8 to rank 1).
     * White pieces occupy ranks 1 and 2, black pieces occupy ranks 7 and 8.
     *
     * @return an array of Piece objects representing the starting position
     */
    public static Piece[] getStartingPieceList() {
        return new Piece[] {
                Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING, Piece.BISHOP, Piece.KNIGHT, Piece.ROOK,
                Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN,
                Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING, Piece.BISHOP, Piece.KNIGHT, Piece.ROOK
        };
    }

    /**
     * Calculates the file (column) of a given square index.
     * Files are numbered 0 (a-file) to 7 (h-file).
     *
     * @param sq the square index (0 to 63)
     * @return the file number (0 to 7)
     */
    public static int getFile(int sq) {
        return sq & 0b000111;
    }

    /**
     * Calculates the rank (row) of a given square index.
     * Ranks are numbered 0 (rank 8) to 7 (rank 1) in rank-major order.
     *
     * @param sq the square index (0 to 63)
     * @return the rank number (0 to 7)
     */
    public static int getRank(int sq) {
        return sq >>> 3;
    }

    /**
     * Determines the diagonal index of a given square.
     * Diagonals are numbered 0 to 14, corresponding to predefined bitboard masks.
     *
     * @param sq the square index (0 to 63)
     * @return the diagonal index (0 to 14)
     * @throws IllegalArgumentException if the square index is invalid
     */
    public static int getDiagonal(int sq) {
        long bb = 1L << sq;
        if ((bb & Bits.DIAGONAL_1) != 0) return 0;
        if ((bb & Bits.DIAGONAL_2) != 0) return 1;
        if ((bb & Bits.DIAGONAL_3) != 0) return 2;
        if ((bb & Bits.DIAGONAL_4) != 0) return 3;
        if ((bb & Bits.DIAGONAL_5) != 0) return 4;
        if ((bb & Bits.DIAGONAL_6) != 0) return 5;
        if ((bb & Bits.DIAGONAL_7) != 0) return 6;
        if ((bb & Bits.DIAGONAL_8) != 0) return 7;
        if ((bb & Bits.DIAGONAL_9) != 0) return 8;
        if ((bb & Bits.DIAGONAL_10) != 0) return 9;
        if ((bb & Bits.DIAGONAL_11) != 0) return 10;
        if ((bb & Bits.DIAGONAL_12) != 0) return 11;
        if ((bb & Bits.DIAGONAL_13) != 0) return 12;
        if ((bb & Bits.DIAGONAL_14) != 0) return 13;
        if ((bb & Bits.DIAGONAL_15) != 0) return 14;
        throw new IllegalArgumentException("Illegal square coordinate " + sq);
    }

    /**
     * Determines the anti-diagonal index of a given square.
     * Anti-diagonals are numbered 0 to 14, corresponding to predefined bitboard masks.
     *
     * @param sq the square index (0 to 63)
     * @return the anti-diagonal index (0 to 14)
     * @throws IllegalArgumentException if the square index is invalid
     */
    public static int getAntiDiagonal(int sq) {
        long bb = 1L << sq;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_1) != 0) return 0;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_2) != 0) return 1;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_3) != 0) return 2;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_4) != 0) return 3;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_5) != 0) return 4;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_6) != 0) return 5;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_7) != 0) return 6;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_8) != 0) return 7;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_9) != 0) return 8;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_10) != 0) return 9;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_11) != 0) return 10;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_12) != 0) return 11;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_13) != 0) return 12;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_14) != 0) return 13;
        if ((bb & Bits.ANTI_DIAGONAL_MASK_15) != 0) return 14;
        throw new IllegalArgumentException("Illegal square coordinate " + sq);
    }

    /**
     * Converts rank and file coordinates to a square index.
     * Assumes rank-major order (rank 0 is rank 8, file 0 is a-file).
     *
     * @param rank the rank number (0 to 7)
     * @param file the file number (0 to 7)
     * @return the square index (0 to 63)
     */
    public static int squareIndex(int rank, int file) {
        return 8 * rank + file;
    }

    /**
     * Checks if a square index is valid.
     * A valid index is between 0 (inclusive) and 64 (exclusive).
     *
     * @param squareIndex the square index to validate
     * @return true if the index is valid, false otherwise
     */
    public static boolean isValidIndex(int squareIndex) {
        return squareIndex >= 0 && squareIndex < 64;
    }

    /**
     * Returns the color index for a given side.
     * Used to index arrays or data structures by color.
     *
     * @param isWhite true for white, false for black
     * @return 1 for white, 0 for black
     */
    public static int getColourIndex(boolean isWhite) {
        return isWhite ? 1 : 0;
    }

    /**
     * Generates a piece list for the current board state.
     * Maps each square to the piece occupying it, if any.
     *
     * @param board the chess board to analyze
     * @return an array of Piece objects, where each index corresponds to a square (0 to 63)
     */
    public static Piece[] calculatePieceList(Board board) {
        Piece[] pieceList = new Piece[64];
        for (int square = 0; square < 64; square++) {
            long squareMask = 1L << square;
            if ((squareMask & (board.getWhitePawns() | board.getBlackPawns())) != 0)          pieceList[square] = Piece.PAWN;
            else if ((squareMask & (board.getWhiteKnights() | board.getBlackKnights())) != 0) pieceList[square] = Piece.KNIGHT;
            else if ((squareMask & (board.getWhiteBishops() | board.getBlackBishops())) != 0) pieceList[square] = Piece.BISHOP;
            else if ((squareMask & (board.getWhiteRooks() | board.getBlackRooks())) != 0)     pieceList[square] = Piece.ROOK;
            else if ((squareMask & (board.getWhiteQueens() | board.getBlackQueens())) != 0)   pieceList[square] = Piece.QUEEN;
            else if ((squareMask & (board.getWhiteKing() | board.getBlackKing())) != 0)       pieceList[square] = Piece.KING;
        }
        return pieceList;
    }

    /**
     * Creates a deep copy of the given chess board.
     * Copies all bitboards, game state, move history, and piece list to ensure independent manipulation.
     *
     * @param board the board to copy
     * @return a new Board instance identical to the input
     */
    public static Board copy(Board board) {
        Board newBoard = new Board();
        newBoard.setWhitePawns(board.getWhitePawns());
        newBoard.setWhiteKnights(board.getWhiteKnights());
        newBoard.setWhiteBishops(board.getWhiteBishops());
        newBoard.setWhiteRooks(board.getWhiteRooks());
        newBoard.setWhiteQueens(board.getWhiteQueens());
        newBoard.setWhiteKing(board.getWhiteKing());
        newBoard.setBlackPawns(board.getBlackPawns());
        newBoard.setBlackKnights(board.getBlackKnights());
        newBoard.setBlackBishops(board.getBlackBishops());
        newBoard.setBlackRooks(board.getBlackRooks());
        newBoard.setBlackQueens(board.getBlackQueens());
        newBoard.setBlackKing(board.getBlackKing());
        newBoard.setWhitePieces(board.getWhitePieces());
        newBoard.setBlackPieces(board.getBlackPieces());
        newBoard.setOccupied(board.getOccupied());
        newBoard.setWhiteToMove(board.isWhiteToMove());
        newBoard.setGameState(board.getGameState().copy());
        Deque<GameState> gameStateHistory = new ArrayDeque<>();
        board.getGameStateHistory().forEach(gameState -> gameStateHistory.add(gameState.copy()));
        newBoard.setGameStateHistory(gameStateHistory);
        Deque<Move> moveHistory = new ArrayDeque<>();
        board.getMoveHistory().forEach(move -> moveHistory.add(new Move(move.getValue())));
        newBoard.setMoveHistory(moveHistory);
        newBoard.setPieceList(Arrays.copyOf(board.getPieceList(), board.getPieceList().length));
        newBoard.recalculatePieces();
        return newBoard;
    }
}