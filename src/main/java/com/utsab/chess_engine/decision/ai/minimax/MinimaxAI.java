package com.utsab.chess_engine.decision.ai.minimax;

import com.utsab.chess_engine.decision.board.Bitboard;
import com.utsab.chess_engine.decision.board.Move;
import com.utsab.chess_engine.decision.board.MoveGenerator;
import com.utsab.chess_engine.decision.board.Bitboard.MoveInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public class MinimaxAI {
    private static final Logger logger = Logger.getLogger(MinimaxAI.class.getName());
    private final MoveGenerator moveGenerator;
    private long nodes;

    public MinimaxAI(MoveGenerator moveGenerator) {
        this.moveGenerator = moveGenerator;
    }

    public record MinimaxResult(Move bestMove, int bestScore, long nodes, List<Move> principalVariation, List<MoveScore> moveList) {}
    public record MoveScore(Move move, int score) {}

    public MinimaxResult findBestMove(Bitboard board, int depth) {
        nodes = 0;
        List<Move> moves = moveGenerator.generateMoves();
        if (moves.isEmpty()) {
            logger.info("No valid moveEntities available.");
            return new MinimaxResult(null, 0, nodes, List.of(), List.of());
        }

        sorter(board, moves);

        Move bestMove = null;
        int bestScore = board.isWhiteToMove() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        List<MoveScore> moveList = new ArrayList<>();
        List<Move> principalVariation = new ArrayList<>();
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Move move : moves) {
            MoveInfo moveInfo = board.makeMove(move);
            List<Move> pv = new ArrayList<>();
            int score = minimax(board, depth - 1, !board.isWhiteToMove(), alpha, beta, pv);
            board.undoMove(moveInfo);

            moveList.add(new MoveScore(move, score));
            logger.fine("MoveEntity " + move + " evaluated with score " + score);

            if (board.isWhiteToMove()) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                    principalVariation.clear();
                    principalVariation.add(move);
                    principalVariation.addAll(pv);
                    alpha = Math.max(alpha, bestScore);
                }
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = move;
                    principalVariation.clear();
                    principalVariation.add(move);
                    principalVariation.addAll(pv);
                    beta = Math.min(beta, bestScore);
                }
            }
        }

        logger.info("Evaluated " + nodes + " nodes. Best move: " +
                (bestMove != null ? bestMove.toString() : "None") +
                " with score: " + bestScore + ", PV: " + principalVariation);
        return new MinimaxResult(bestMove, bestScore, nodes, principalVariation, moveList);
    }

    private void sorter(Bitboard board, List<Move> moves) {
        moves.sort(Comparator.comparingInt(move -> {
            MoveInfo info = board.makeMove(move);
            int captureValue = Evaluation.captureValue(info);
            boolean isCheck = board.isInCheck(!board.isWhiteToMove());
            board.undoMove(info);
            return -(captureValue + (isCheck ? 1000 : 0));
        }));
    }

    private int minimax(Bitboard board, int depth, boolean isMaximizing, int alpha, int beta, List<Move> pv) {
        nodes++;
        if (depth == 0) {
            int score = Evaluation.evaluate(board);
            logger.fine("Leaf node evaluated: " + score + " for board:\n" + board);
            return score;
        }

        List<Move> moves = moveGenerator.generateMoves();
        if (moves.isEmpty()) {
            if (board.isInCheck(board.isWhiteToMove())) {
                int score = isMaximizing ? -10000 : 10000;
                logger.fine("Checkmate detected, score: " + score);
                return score;
            }
            logger.fine("Stalemate detected, score: 0");
            return 0;
        }

        sorter(board, moves);

        int bestEval = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        List<Move> bestPv = new ArrayList<>();

        for (Move move : moves) {
            MoveInfo moveInfo = board.makeMove(move);
            List<Move> subPv = new ArrayList<>();
            int eval = minimax(board, depth - 1, !isMaximizing, alpha, beta, subPv);
            board.undoMove(moveInfo);

            if (isMaximizing) {
                if (eval > bestEval) {
                    bestEval = eval;
                    bestPv.clear();
                    bestPv.add(move);
                    bestPv.addAll(subPv);
                }
                alpha = Math.max(alpha, bestEval);
                if (beta <= alpha) {
                    break; // Beta cutoff
                }
            } else {
                if (eval < bestEval) {
                    bestEval = eval;
                    bestPv.clear();
                    bestPv.add(move);
                    bestPv.addAll(subPv);
                }
                beta = Math.min(beta, bestEval);
                if (beta <= alpha) {
                    break; // Alpha cutoff
                }
            }
        }

        pv.addAll(bestPv);
        return bestEval;
    }
}