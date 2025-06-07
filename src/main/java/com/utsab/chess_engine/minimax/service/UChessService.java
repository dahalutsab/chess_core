package com.utsab.chess_engine.minimax.service;

import com.utsab.chess_engine.minimax.dto.UChessRequest;
import com.utsab.chess_engine.minimax.dto.UChessResponse;

public interface UChessService {
    UChessResponse getBestMove(UChessRequest uChessRequest);
}
