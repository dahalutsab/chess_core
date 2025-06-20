package com.utsab.chess_engine.minimax.service;

import com.utsab.chess_engine.game.dto.GameDto;
import com.utsab.chess_engine.minimax.dto.NewGameRequest;
import com.utsab.chess_engine.minimax.dto.UChessRequest;
import com.utsab.chess_engine.minimax.dto.UChessResponse;

import java.util.UUID;

public interface UChessService {
    GameDto startNewGame(NewGameRequest request);

    UChessResponse getBestMove(UChessRequest uChessRequest);

    GameDto getGameInfo(UUID gameId);
}
