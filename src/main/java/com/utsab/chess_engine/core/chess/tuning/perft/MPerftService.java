package com.utsab.chess_engine.core.chess.tuning.perft;

import com.utsab.chess_engine.core.chess.board.Board;
import com.utsab.chess_engine.core.chess.board.Move;
import com.utsab.chess_engine.core.chess.generation.MoveGenerator;
import com.utsab.chess_engine.core.chess.search.moveordering.MoveOrderer;

import java.util.List;

public class MPerftService {

    private final MoveGenerator moveGenerator = new MoveGenerator();
    private final MoveOrderer moveOrderer = new MoveOrderer();

    public int perft(Board board, int depth) {
        List<Move> moves = moveGenerator.generateMoves(board);
        moves = moveOrderer.orderMoves(board, moves, null, true, 0);
        if (depth == 1) {
            return moves.size();
        }
        int totalMoveCount = 0;
        for (Move move : moves) {
            board.makeMove(move);
            totalMoveCount += perft(board, depth - 1);
            board.unmakeMove();
        }
        return totalMoveCount;
    }

}
