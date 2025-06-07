package com.utsab.chess_engine.core.chess.tuning.perft;

import com.utsab.chess_engine.core.chess.board.Board;
import com.utsab.chess_engine.core.chess.board.Move;
import com.utsab.chess_engine.core.chess.engine.EngineInitializer;
import com.utsab.chess_engine.core.chess.evaluation.Evaluator;
import com.utsab.chess_engine.core.chess.generation.MoveGenerator;

import java.util.List;

public class EPerftService {

    private final MoveGenerator moveGenerator;
    private final Evaluator evaluator;

    public EPerftService(Board board) {
        moveGenerator = new MoveGenerator();
        evaluator = new Evaluator(EngineInitializer.loadDefaultConfig());
        evaluator.evaluate(board);
    }

    public void ePerft(Board board, int depth) {
        if (depth == 0) {
            return;
        }
        List<Move> moves = moveGenerator.generateMoves(board);
        for (Move move : moves) {
            board.makeMove(move);
            evaluator.evaluate(board);
            ePerft(board, depth - 1);
            board.unmakeMove();
        }
    }

}
