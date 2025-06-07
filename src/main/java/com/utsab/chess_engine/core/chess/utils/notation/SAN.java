package com.utsab.chess_engine.core.chess.utils.notation;

/**
 * A utility class to convert chess moves into Standard Algebraic Notation (SAN).
 * SAN is a widely used notation to represent chess moves in a human-readable format (e.g., "Nf3", "Qxe5+").
 * This implementation ensures moves are converted before they are applied to the board to maintain accuracy.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025-06-07
 * @see <a href="https://www.chessprogramming.org/Algebraic_Chess_Notation">Chess Programming Wiki - Algebraic Notation</a>
 */
import com.utsab.chess_engine.core.chess.board.Board;
import com.utsab.chess_engine.core.chess.board.Move;
import com.utsab.chess_engine.core.chess.board.Piece;
import com.utsab.chess_engine.core.chess.generation.MoveGenerator;
import com.utsab.chess_engine.core.chess.utils.BoardUtils;

import java.util.List;

public class SAN {

    /**
     * Convert move to Standard Algebraic Notation (SAN).
     * Note: the move must not yet have been made on the board to ensure accurate state evaluation.
     *
     * @param move the chess move to convert
     * @param board the current state of the chessboard
     * @return the SAN string representation (e.g., "Nf3", "Qxe5#")
     * @throws IllegalArgumentException if the move or board state is invalid
     */
    public static String toSan(Move move, Board board) {
        Piece pieceType = board.pieceAt(move.getStartSquare());
        Piece capturedPieceType = board.pieceAt(move.getEndSquare());

        if (move.isCastling()) {
            int delta = move.getEndSquare() - move.getStartSquare();
            return delta == 2 ? "O-O" : "O-O-O";
        }

        MoveGenerator moveGenerator = new MoveGenerator();
        String notation = "";
        if (pieceType != Piece.PAWN) {
            notation += Notation.PIECE_NOTATION_MAP.get(pieceType).toUpperCase();
        }

        // Check for ambiguity in notation (e.g., multiple pieces moving to the same square)
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

        if (capturedPieceType != null) {
            // Add 'x' to indicate capture
            if (pieceType == Piece.PAWN) {
                notation += Notation.getFile(move.getStartSquare());
            }
            notation += "x";
        } else {
            // Check if capturing en passant
            if (move.isEnPassant()) {
                notation += Notation.getFile(move.getStartSquare()) + "x";
            }
        }

        notation += Notation.getFile(move.getEndSquare());
        notation += Notation.getRank(move.getEndSquare());

        // Add promotion piece type if applicable
        if (move.isPromotion()) {
            Piece promotionPieceType = move.getPromotionPieceType();
            notation += "=" + Notation.PIECE_NOTATION_MAP.get(promotionPieceType).toUpperCase();
        }

        board.makeMove(move);
        if (moveGenerator.isCheck(board, board.isWhiteToMove())) {
            List<Move> legalMoves = moveGenerator.generateMoves(board);
            notation += legalMoves.isEmpty() ? "#" : "+";
        }
        board.unmakeMove();

        return notation;
    }
}