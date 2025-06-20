package com.utsab.chess_engine.core.utils.notation;

import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.Move;
import com.utsab.chess_engine.core.board.Piece;
import com.utsab.chess_engine.core.generation.MoveGenerator;
import com.utsab.chess_engine.core.utils.BoardUtils;

import java.util.List;

/**
 * A utility class for converting chess moveEntities into Standard Algebraic Notation (SAN), a human-readable
 * format for representing chess moveEntities (e.g., "Nf3", "Qxe5+"). This implementation ensures accurate
 * conversion by evaluating moveEntities before they are applied施

 System: Applied to the board, maintaining compatibility with UCI protocols.

 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Algebraic_Chess_Notation">Chess Programming Wiki - Algebraic Notation</a>
 */
public class SAN {

    /**
     * Converts a chess move to its Standard Algebraic Notation (SAN) representation.
     *
     * <p>The method evaluates the move in the context of the current board state to ensure accurate
     * notation, including handling castling, captures, promotions, checks, and checkmates. The move
     * must not yet have been applied to the board to maintain state consistency.
     *
     * <p>SAN notation includes:
     * <ul>
     *     <li>Piece type (e.g., "N" for knight, omitted for pawns)</li>
     *     <li>File or rank for disambiguation if multiple pieces can move to the same square</li>
     *     <li>"x" for captures</li>
     *     <li>"=" followed by piece type for promotions (e.g., "=Q")</li>
     *     <li>"+" for check, "#" for checkmate</li>
     *     <li>"O-O" or "O-O-O" for castling</li>
     * </ul>
     *
     * @param move the chess move to convert
     * @param board the current state of the chessboard
     * @return the SAN string representation (e.g., "Nf3", "Qxe5#")
     * @throws IllegalArgumentException if the move or board state is invalid
     */
    public static String toSan(Move move, Board board) {
        // Retrieve piece types for the move
        Piece pieceType = board.pieceAt(move.getStartSquare());
        Piece capturedPieceType = board.pieceAt(move.getEndSquare());

        // Handle castling moveEntities
        if (move.isCastling()) {
            int delta = move.getEndSquare() - move.getStartSquare();
            return delta == 2 ? "O-O" : "O-O-O";
        }

        // Initialize notation with piece type (if not pawn)
        MoveGenerator moveGenerator = new MoveGenerator();
        String notation = "";
        if (pieceType != Piece.PAWN) {
            notation += Notation.PIECE_NOTATION_MAP.get(pieceType).toUpperCase();
        }

        // Check for ambiguity in piece moveEntities
        if (pieceType != Piece.PAWN && pieceType != Piece.KING) {
            List<Move> legalMoves = moveGenerator.generateMoves(board);

            for (Move legalMove : legalMoves) {
                if (legalMove.getStartSquare() != move.getStartSquare() && legalMove.getEndSquare() == move.getEndSquare()) {
                    if (board.pieceAt(legalMove.getStartSquare()) == pieceType) {
                        int fromFileIndex = BoardUtils.getFile(move.getStartSquare());
                        int alternateFromFileIndex = BoardUtils.getFile(legalMove.getEndSquare());
                        int fromRankIndex = BoardUtils.getRank(move.getStartSquare());
                        int alternateFromRankIndex = BoardUtils.getRank(legalMove.getStartSquare());

                        if (fromFileIndex != alternateFromFileIndex) {
                            notation += Notation.getFile(fromFileIndex);
                            break;
                        } else if (fromRankIndex != alternateFromRankIndex) {
                            notation += Notation.getRank(fromRankIndex);
                            break;
                        }
                    }
                }
            }
        }

        // Handle captures and en passant
        if (capturedPieceType != null) {
            if (pieceType == Piece.PAWN) {
                notation += Notation.getFile(move.getStartSquare());
            }
            notation += "x";
        } else if (move.isEnPassant()) {
            notation += Notation.getFile(move.getStartSquare()) + "x";
        }

        // Append destination square
        notation += Notation.getFile(move.getEndSquare());
        notation += Notation.getRank(move.getEndSquare());

        // Handle promotions
        if (move.isPromotion()) {
            Piece promotionPieceType = move.getPromotionPieceType();
            notation += "=" + Notation.PIECE_NOTATION_MAP.get(promotionPieceType).toUpperCase();
        }

        // Check for check or checkmate
        board.makeMove(move);
        if (moveGenerator.isCheck(board, board.isWhiteToMove())) {
            List<Move> legalMoves = moveGenerator.generateMoves(board);
            notation += legalMoves.isEmpty() ? "#" : "+";
        }
        board.unmakeMove();

        return notation;
    }
}