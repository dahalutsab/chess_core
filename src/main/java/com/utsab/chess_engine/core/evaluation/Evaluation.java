package com.utsab.chess_engine.core.evaluation;

import com.utsab.chess_engine.core.board.Board;

public interface Evaluation {

    int evaluate(Board board);

    Score getScore();

}
