package com.utsab.chess_engine.core.utils.notation;

import com.utsab.chess_engine.core.board.Move;
import com.utsab.chess_engine.core.board.Piece;

import java.util.List;
import java.util.Map;

/**
 * A utility class for translating between algebraic notation and internal move representations
 * in a UCI-compatible chess engine. Supports conversion of square coordinates (e.g., "e4" to 28)
 * and move notations (e.g., "e2e4" for UCI). This class is critical for parsing and generating
 * move notations used in UCI communication.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Algebraic_Chess_Notation">Chess Programming Wiki - Algebraic Notation</a>
 */
public class Notation {

    /**
     * A mapping of piece types to their algebraic notation symbols (e.g., {@link Piece#KNIGHT} to "n").
     */
    public static final Map<Piece, String> PIECE_NOTATION_MAP = Map.of(
            Piece.PAWN, "p",
            Piece.KNIGHT, "n",
            Piece.BISHOP, "b",
            Piece.ROOK, "r",
            Piece.QUEEN, "q",
            Piece.KING, "k"
    );

    /**
     * Converts a UCI combined algebraic notation string (e.g., "e2e4" or "a7a8q") into a {@link Move} object.
     * Supports promotion moveEntities by including the promotion piece type (e.g., "q" for queen).
     *
     * @param notation the UCI notation string (e.g., "e2e4" or "a7a8q")
     * @return a new {@link Move} instance representing the parsed move
     * @throws IllegalArgumentException if the notation is invalid or malformed
     * @author Utsab Dahal
     */
    public static Move fromCombinedNotation(String notation) {
        // Extract start and end squares from notation
        int startSquare = fromNotation(notation.substring(0, 2));
        int endSquare = fromNotation(notation.substring(2, 4));

        // Handle promotion if present
        int flag = Move.NO_FLAG;
        if (notation.length() == 5) {
            String pieceCode = notation.substring(4, 5);
            Piece promotionPieceType = PIECE_NOTATION_MAP.entrySet().stream()
                    .filter(entry -> entry.getValue().equalsIgnoreCase(pieceCode))
                    .findAny().orElseThrow(() -> new IllegalArgumentException("Invalid promotion piece: " + pieceCode))
                    .getKey();
            flag = Move.getPromotionFlag(promotionPieceType);
        }
        return new Move(startSquare, endSquare, flag);
    }


    //from san notation


    /**
     * Converts a {@link Move} object to its UCI algebraic notation representation.
     *
     * @param move the move to convert, or {@code null} to return "-"
     * @return the UCI notation string (e.g., "e2e4" or "a7a8q")
     * @author Utsab Dahal
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
     * Converts an algebraic square notation (e.g., "e4") to a square index (0-63).
     *
     * @param algebraic the algebraic notation for a square (e.g., "e4")
     * @return the square index (0-63)
     * @throws IllegalArgumentException if the notation is invalid
     * @author Utsab Dahal
     */
    public static int fromNotation(String algebraic) {
        int xOffset = List.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h').indexOf(algebraic.charAt(0));
        int yAxis = (Integer.parseInt(Character.valueOf(algebraic.charAt(1)).toString()) - 1) * 8;
        return yAxis + xOffset;
    }

    /**
     * Converts a square index (0-63) to its algebraic notation (e.g., 28 to "e4").
     *
     * @param square the square index (0-63)
     * @return the algebraic notation (e.g., "e4")
     * @author Utsab Dahal
     */
    public static String toNotation(int square) {
        return getFile(square) + getRank(square);
    }

    /**
     * Retrieves the rank (1-8) for a given square index.
     *
     * @param square the square index (0-63)
     * @return the rank as a string (e.g., "4")
     * @author Utsab Dahal
     */
    public static String getRank(int square) {
        return switch (square / 8) {
            case 0 -> "1";
            case 1 -> "2";
            case 2 -> "3";
            case 3 -> "4";
            case 4 -> "5";
            case 5 -> "6";
            case 6 -> "7";
            case 7 -> "8";
            default -> throw new IllegalArgumentException("Invalid square index: " + square);
        };
    }

    /**
     * Retrieves the file (a-h) for a given square index.
     *
     * @param square the square index (0-63)
     * @return the file as a string (e.g., "e")
     * @author Utsab Dahal
     */
    public static String getFile(int square) {
        return switch (square & 7) {
            case 0 -> "a";
            case 1 -> "b";
            case 2 -> "c";
            case 3 -> "d";
            case 4 -> "e";
            case 5 -> "f";
            case 6 -> "g";
            case 7 -> "h";
            default -> throw new IllegalArgumentException("Invalid square index: " + square);
        };
    }
}