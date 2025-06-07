package com.utsab.chess_engine.core.chess.utils.notation;

import com.utsab.chess_engine.core.chess.board.Bits;
import com.utsab.chess_engine.core.chess.board.Move;
import com.utsab.chess_engine.core.chess.board.Piece;

import java.util.*;

/**
 * A utility class for translating between algebraic notation and internal move representations.
 * Supports conversion of square coordinates (e.g., "e4" to 28) and move notations (e.g., "e2e4" for UCI).
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025-06-07
 * @see <a href="https://www.chessprogramming.org/Algebraic_Chess_Notation">Chess Programming Wiki - Algebraic Notation</a>
 */
public class Notation {

    public static final Map<Piece, String> PIECE_NOTATION_MAP = Map.of(
            Piece.PAWN, "p",
            Piece.KNIGHT, "n",
            Piece.BISHOP, "b",
            Piece.ROOK, "r",
            Piece.QUEEN, "q",
            Piece.KING, "k"
    );

    /**
     * Generate a {@link Move} from algebraic notation of the start and end square (e.g. "e2", "e4" -> new Move(12, 28)).
     *
     * @param startSquare the starting square in algebraic notation (e.g., "e2")
     * @param endSquare the ending square in algebraic notation (e.g., "e4")
     * @return a new Move instance
     * @throws IllegalArgumentException if the notation is invalid
     */
    public static Move fromNotation(String startSquare, String endSquare) {
        return new Move(fromNotation(startSquare), fromNotation(endSquare));
    }

    /**
     * Generate a {@link Move} from algebraic notation with a specific flag (e.g., promotion).
     *
     * @param startSquare the starting square in algebraic notation (e.g., "e2")
     * @param endSquare the ending square in algebraic notation (e.g., "e4")
     * @param flag the move flag (e.g., promotion type)
     * @return a new Move instance with the specified flag
     * @throws IllegalArgumentException if the notation is invalid
     */
    public static Move fromNotation(String startSquare, String endSquare, int flag) {
        return new Move(fromNotation(startSquare), fromNotation(endSquare), flag);
    }

    /**
     * Generate a {@link Move} from combined algebraic notation (e.g., "e2e4"), as used in the UCI protocol.
     * Special case promotion: "a2a1q" - values 'q' | 'b' | 'r' | 'n'.
     *
     * @param notation the combined notation string (e.g., "e2e4" or "a7a8q")
     * @return a new Move instance
     * @throws IllegalArgumentException if the notation is invalid
     */
    public static Move fromCombinedNotation(String notation) {
        int startSquare = fromNotation(notation.substring(0, 2));
        int endSquare = fromNotation(notation.substring(2, 4));

        int flag = Move.NO_FLAG;
        if (notation.length() == 5) {
            String pieceCode = notation.substring(4, 5);
            Piece promotionPieceType = PIECE_NOTATION_MAP.entrySet().stream()
                    .filter(entry -> entry.getValue().equalsIgnoreCase(pieceCode))
                    .findAny().orElseThrow().getKey();
            flag = Move.getPromotionFlag(promotionPieceType);
        }
        return new Move(startSquare, endSquare, flag);
    }

    /**
     * Convert a {@link Move} to its algebraic notation representation.
     *
     * @param move the move to convert
     * @return the algebraic notation string (e.g., "e2e4" or "a7a8q")
     */
    public static String toNotation(Move move) {
        if (move == null) {
            return "-";
        }
        String notation = toNotation(move.getStartSquare()) + toNotation(move.getEndSquare());
        if (move.getPromotionPieceType() != null) {
            notation += PIECE_NOTATION_MAP.get(move.getPromotionPieceType());
        }
        return notation;
    }

    /**
     * Convert a deque of moves to a string representation of algebraic notation.
     *
     * @param moveHistory the deque of moves to convert
     * @return a string of concatenated algebraic notations
     */
    public static String toNotation(Deque<Move> moveHistory) {
        List<Move> moves = new ArrayList<>(moveHistory);
        Collections.reverse(moves);
        return moves.stream()
                .map(Notation::toNotation)
                .toList()
                .toString();
    }

    /**
     * Generate a square coordinate from algebraic notation (e.g., "e4" -> 28).
     *
     * @param algebraic the algebraic notation (e.g., "e4")
     * @return the square index (0-63)
     * @throws IllegalArgumentException if the notation is invalid
     */
    public static int fromNotation(String algebraic) {
        int xOffset = List.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h').indexOf(algebraic.charAt(0));
        int yAxis = (Integer.parseInt(Character.valueOf(algebraic.charAt(1)).toString()) - 1) * 8;
        return yAxis + xOffset;
    }

    /**
     * Convert a square index to algebraic notation (e.g., 28 -> "e4").
     *
     * @param square the square index (0-63)
     * @return the algebraic notation (e.g., "e4")
     */
    public static String toNotation(int square) {
        return getFile(square) + getRank(square);
    }

    /**
     * Get the file letter for a given square index.
     *
     * @param square the square index (0-63)
     * @return the file letter (e.g., "e")
     */
    public static String getRank(int square) {
        int rank = square / 8;
        return switch (rank) {
            case 0 -> "1";
            case 1 -> "2";
            case 2 -> "3";
            case 3 -> "4";
            case 4 -> "5";
            case 5 -> "6";
            case 6 -> "7";
            case 7 -> "8";
            default -> "X";
        };
    }

    /**
     * Get the file letter for a given square index.
     *
     * @param square the square index (0-63)
     * @return the file letter (e.g., "e")
     */
    public static String getFile(int square) {
        int file = square & 7;
        return switch (file) {
            case 0 -> "a";
            case 1 -> "b";
            case 2 -> "c";
            case 3 -> "d";
            case 4 -> "e";
            case 5 -> "f";
            case 6 -> "g";
            case 7 -> "h";
            default -> "X";
        };
    }
}