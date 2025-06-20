package com.utsab.chess_engine.core.opening;

import com.utsab.chess_engine.core.board.Move;

public record BookMove(Move move, int frequency) {}
