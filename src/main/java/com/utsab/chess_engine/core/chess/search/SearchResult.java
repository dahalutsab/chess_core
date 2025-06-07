package com.utsab.chess_engine.core.chess.search;

import com.utsab.chess_engine.core.chess.board.Move;

public record SearchResult(int eval, Move move, int depth, long time, int nodes, long nps) { }
