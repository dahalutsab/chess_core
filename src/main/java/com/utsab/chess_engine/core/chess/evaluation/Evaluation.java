package com.utsab.chess_engine.core.chess.evaluation;

import com.utsab.chess_engine.core.chess.board.Board;

public interface Evaluation {

    int evaluate(Board board);

    Score getScore();

}
