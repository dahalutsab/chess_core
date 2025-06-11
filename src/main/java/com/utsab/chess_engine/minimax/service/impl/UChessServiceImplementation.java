package com.utsab.chess_engine.minimax.service.impl;

import com.utsab.chess_engine.core.chess.Core;
import com.utsab.chess_engine.core.chess.search.SearchResult;
import com.utsab.chess_engine.core.chess.utils.notation.Notation;
import com.utsab.chess_engine.core.chess.utils.notation.SAN;
import com.utsab.chess_engine.minimax.dto.UChessRequest;
import com.utsab.chess_engine.minimax.dto.UChessResponse;
import com.utsab.chess_engine.minimax.service.UChessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Implementation of the UChessService interface.
 * This service is responsible for handling chess-related operations.
 * @author Utsab Dahal
 * @version 1.0
 * @since 2023-10-01
 */
@Service
@RequiredArgsConstructor
public class UChessServiceImplementation implements UChessService {

    @Override
    public UChessResponse getBestMove(UChessRequest uChessRequest) {
        String fen = uChessRequest.getFen();
        String gameId = uChessRequest.getGameId();

        // Set the position in the engine
        Core.ENGINE.setPosition(fen, new ArrayList<>());

        // Think for a default time (e.g., 1000ms)
        SearchResult result =
                Core.ENGINE.thinkSync(1000);

        String bestMove = Notation.toNotation(result.move());
        String bestMoveSan = SAN.toSan(result.move(), Core.ENGINE.getBoard());

        return new UChessResponse(gameId, fen, bestMove, bestMoveSan);
    }
}
