package com.utsab.chess_engine.negamax.controller;

import com.utsab.chess_engine.common.BaseController;
import com.utsab.chess_engine.game.dto.GameDto;
import com.utsab.chess_engine.game.dto.history.HistoryResponse;
import com.utsab.chess_engine.negamax.dto.NewGameRequest;
import com.utsab.chess_engine.negamax.dto.UChessRequest;
import com.utsab.chess_engine.negamax.dto.UChessResponse;
import com.utsab.chess_engine.negamax.service.UChessService;
import com.utsab.chess_engine.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.utsab.chess_engine.common.constants.APIConstants.API_BASE;

@RestController
@RequestMapping(API_BASE + "/game")
@RequiredArgsConstructor
public class UChessController extends BaseController {
    private final UChessService uChessService;

    @PostMapping("/new")
    public ResponseEntity<ApiResponse<GameDto>> getBestMove(@RequestBody NewGameRequest newGameRequest) {
        GameDto gameDto = uChessService.startNewGame(newGameRequest);
        return successResponse(gameDto, "New game started successfully");
    }

    @PostMapping("/best-move")
    public ResponseEntity<ApiResponse<UChessResponse>> getBestMove(@RequestBody UChessRequest uChessRequest) {
        UChessResponse uChessResponse = uChessService.getBestMove(uChessRequest);
        return successResponse(uChessResponse, "Best move calculated successfully");
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<ApiResponse<GameDto>> getGameInfo(@PathVariable UUID gameId) {
        GameDto gameDto = uChessService.getGameInfo(gameId);
        return successResponse(gameDto, "Game info fetched successfully");
    }

    @PostMapping("/{gameId}/resign")
    public ResponseEntity<ApiResponse<GameDto>> resign(@PathVariable UUID gameId) {
        return successResponse(uChessService.resign(gameId), "Game resigned");
    }

    @GetMapping("/rating-history")
    public ResponseEntity<ApiResponse<List<UChessService.RatingPoint>>> getRatingHistory() {
        return successResponse(uChessService.getRatingHistory(), "Rating history fetched successfully");
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<HistoryResponse>> getGameHistory(Pageable pageable) {
        HistoryResponse history = uChessService.getGameHistory(pageable);
        return successResponse(history, "Game history fetched successfully");
    }
}
