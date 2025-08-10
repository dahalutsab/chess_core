package com.utsab.chess_engine.negamax.service;

import com.utsab.chess_engine.game.dto.GameDto;
import com.utsab.chess_engine.game.dto.history.HistoryResponse;
import com.utsab.chess_engine.negamax.dto.NewGameRequest;
import com.utsab.chess_engine.negamax.dto.UChessRequest;
import com.utsab.chess_engine.negamax.dto.UChessResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UChessService {
    GameDto startNewGame(NewGameRequest request);

    UChessResponse getBestMove(UChessRequest uChessRequest);

    GameDto getGameInfo(UUID gameId);

    HistoryResponse getGameHistory(Pageable pageable);
}
