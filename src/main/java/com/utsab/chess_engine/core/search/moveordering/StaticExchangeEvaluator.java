package com.utsab.chess_engine.core.search.moveordering;

import com.utsab.chess_engine.core.board.BitwiseOperations;
import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.Move;
import com.utsab.chess_engine.core.board.Piece;
import com.utsab.chess_engine.core.generation.MoveGenerator;
import com.utsab.chess_engine.core.utils.BoardUtils;

/**
 * Performs Static Exchange Evaluation (SEE) to estimate material changes after a series of captures on a single square.
 * Used in quiescence search to prune detrimental captures, optimizing search speed in a UCI-compatible chess engine.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Static_Exchange_Evaluation">Chess Programming Wiki - Static Exchange Evaluation</a>
 */
public class StaticExchangeEvaluator {

    private final MoveGenerator moveGenerator = new MoveGenerator();

    /**
     * Evaluates the material change after a capture sequence initiated by a move.
     *
     * @param board the current board position
     * @param move  the capture move to evaluate
     * @return the net material change (positive for gain, negative for loss)
     * @throws NullPointerException if board or move is null
     * @throws IllegalArgumentException if move's end square is invalid
     */
    public int evaluate(Board board, Move move) {

        int score = 0;
        int square = move.getEndSquare();
        Piece capturedPiece = move.isEnPassant() ? Piece.PAWN : board.pieceAt(square);
        score += capturedPiece != null ? capturedPiece.getValue() : 0;

        board.makeMove(move);
        Move leastValuableAttacker = getLeastValuableAttacker(board, square);
        if (leastValuableAttacker != null) {
            /* The opponent should have the option of 'standing pat' - that is, declining to continue the capture
             sequence if it would lead to a loss of material.
             Therefore, we return the minimum of the stand-pat score and the capture score. */
            score = Math.min(score, score - evaluate(board, leastValuableAttacker));
        }
        board.unmakeMove();
        return score;

    }

    /**
     * Evaluates the material change after a capture sequence, assuming the initial move has been made.
     *
     * @param board the current board position (post-move)
     * @param move  the capture move already made
     * @return the net material change (positive for gain, negative for loss)
     * @throws NullPointerException if board or move is null
     * @throws IllegalArgumentException if move's end square is invalid
     */
    public int evaluateAfterMove(Board board, Move move) {

        int score = 0;
        int square = move.getEndSquare();
        Piece capturedPiece = board.getGameState().getCapturedPiece();
        score += capturedPiece != null ? capturedPiece.getValue() : 0;

        Move leastValuableAttacker = getLeastValuableAttacker(board, square);
        if (leastValuableAttacker != null) {
            /* The opponent should have the option of 'standing pat' - that is, declining to continue the capture
             sequence if it would lead to a loss of material.
             Therefore, we return the minimum of the stand-pat score and the capture score. */
            score = Math.min(score, score - evaluate(board, leastValuableAttacker));
        }
        return score;

    }


    /**
     * Finds the least valuable piece attacking a square for the side to move.
     *
     * @param board the current board position
     * @param square the target square
     * @return the move of the least valuable attacker, or null if none
     * @throws IllegalArgumentException if square is invalid
     */
    private Move getLeastValuableAttacker(Board board, int square) {
        if (!BoardUtils.isValidIndex(square)) {
            throw new IllegalArgumentException("Invalid square index: " + square);
        }

        boolean isWhite = board.isWhiteToMove();
        for (Piece piece : new Piece[]{Piece.PAWN, Piece.KNIGHT, Piece.BISHOP, Piece.ROOK, Piece.QUEEN, Piece.KING}) {
            long pieces = switch (piece) {
                case PAWN -> board.getPawns(isWhite);
                case KNIGHT -> board.getKnights(isWhite);
                case BISHOP -> board.getBishops(isWhite);
                case ROOK -> board.getRooks(isWhite);
                case QUEEN -> board.getQueens(isWhite);
                case KING -> board.getKing(isWhite);
            };
            if (pieces == 0) continue;

            long attackMask = switch (piece) {
                case PAWN -> moveGenerator.getPawnAttacks(board, square, !isWhite);
                case KNIGHT -> moveGenerator.getKnightAttacks(board, square, !isWhite);
                case BISHOP -> moveGenerator.getBishopAttacks(board, square, !isWhite);
                case ROOK -> moveGenerator.getRookAttacks(board, square, !isWhite);
                case QUEEN -> moveGenerator.getQueenAttacks(board, square, !isWhite);
                case KING -> moveGenerator.getKingAttacks(board, square, !isWhite);
            };
            if ((attackMask & pieces) != 0) {
                int startSquare = BitwiseOperations.getNextBit(attackMask & pieces);
                return new Move(startSquare, square);
            }
        }
        return null;
    }

}
