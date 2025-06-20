package com.utsab.chess_engine.core.utils.notation;

import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.Move;
import com.utsab.chess_engine.core.board.Piece;

/**
 * A utility class for converting chess moveEntities into Coordinate Notation,
 * also known as Long Algebraic Notation (e.g., "e2e4", "g1f3", "e7e8q").
 * This is a concise, engine-friendly format widely used in UCI protocols.
 *
 * Coordinate notation always uses:
 * - source square (e.g., e2)
 * - target square (e.g., e4)
 * - optional promotion piece (e.g., q, r, b, n)
 *
 * It does not include checks, captures, or castling symbols.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Algebraic_Chess_Notation">Chess Programming Wiki</a>
 */
public class CoordinateNotation {

    /**
     * Converts a move to coordinate notation string (e.g., "e2e4", "e7e8q").
     *
     * @param move  the chess move to convert
     * @param board the current board (used if needed for promotion info)
     * @return the coordinate notation string
     * @throws IllegalArgumentException if the move is invalid
     */
    public static String toCoordinate(Move move, Board board) {
        if (move == null || board == null) {
            throw new IllegalArgumentException("Move or board cannot be null.");
        }

        StringBuilder notation = new StringBuilder();

        notation.append(Notation.getFile(move.getStartSquare()));
        notation.append(Notation.getRank(move.getStartSquare()));
        notation.append(Notation.getFile(move.getEndSquare()));
        notation.append(Notation.getRank(move.getEndSquare()));

        // Append promotion piece in lowercase (q, r, b, n) — UCI standard
        if (move.isPromotion()) {
            Piece promotionPiece = move.getPromotionPieceType();
            String symbol = Notation.PIECE_NOTATION_MAP.get(promotionPiece).toLowerCase();
            notation.append(symbol);
        }

        return notation.toString();
    }
}
