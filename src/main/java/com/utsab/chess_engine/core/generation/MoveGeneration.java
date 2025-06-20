package com.utsab.chess_engine.core.generation;

import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.Move;

import java.util.List;

public interface MoveGeneration {

    List<Move> generateMoves(Board board);

    List<Move> generateMoves(Board board, MoveFilter filter);

    boolean isCheck(Board board, boolean isWhite);

    enum MoveFilter {
        ALL,
        CAPTURES_AND_CHECKS,
        CAPTURES_ONLY
    }

}
