package com.utsab.chess_engine.ai.minimax;

import com.utsab.chess_engine.board.Bitboard;
import com.utsab.chess_engine.board.Move;
import com.utsab.chess_engine.board.MoveGenerator;
import com.utsab.chess_engine.board.Bitboard.MoveInfo;

import java.util.List;
import java.util.logging.Logger;

public class MinimaxAI {
    private static final Logger logger = Logger.getLogger(MinimaxAI.class.getName());
    private final MoveGenerator moveGenerator;

    public MinimaxAI(MoveGenerator moveGenerator) {
        this.moveGenerator = moveGenerator;
    }

    /**
     * Finds the best move using the minimax algorithm up to a specified depth.
     */
    public Move findBestMove(Bitboard board, int depth) {
        List<Move> moves = moveGenerator.generateMoves();
        if (moves.isEmpty()) {
            logger.info("No valid moves available.");
            return null;
        }

        Move bestMove = null;
        int bestScore = board.isWhiteToMove() ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Move move : moves) {
            MoveInfo moveInfo = board.makeMove(move);
            int score = minimax(board, depth - 1, !board.isWhiteToMove());
            board.undoMove(moveInfo);

            if (board.isWhiteToMove()) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
        }
        logger.info("Best move: " + (bestMove != null ? bestMove.toString() : "None") + " with score: " + bestScore);
        return bestMove;
    }

    /**
     * Recursive minimax algorithm with depth limiting.
     */
    private int minimax(Bitboard board, int depth, boolean isMaximizing) {
        if (depth == 0) {
            int score = Evaluation.evaluate(board);
            logger.info("Evaluated position at depth 0: " + score);
            return score;
        }

        List<Move> moves = moveGenerator.generateMoves();
        if (moves.isEmpty()) {
            if (board.isInCheck(board.isWhiteToMove())) {
                int score = isMaximizing ? -10000 : 10000;
                logger.info("Checkmate detected, score: " + score);
                return score;
            }
            logger.info("Stalemate detected, score: 0");
            return 0;
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                MoveInfo moveInfo = board.makeMove(move);
                int eval = minimax(board, depth - 1, false);
                board.undoMove(moveInfo);
                maxEval = Math.max(maxEval, eval);
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : moves) {
                MoveInfo moveInfo = board.makeMove(move);
                int eval = minimax(board, depth - 1, true);
                board.undoMove(moveInfo);
                minEval = Math.min(minEval, eval);
            }
            return minEval;
        }
    }
}