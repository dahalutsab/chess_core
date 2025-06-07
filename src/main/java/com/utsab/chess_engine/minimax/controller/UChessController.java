package com.utsab.chess_engine.minimax.controller;

//javadoc


import com.utsab.chess_engine.common.BaseController;
import com.utsab.chess_engine.minimax.dto.UChessRequest;
import com.utsab.chess_engine.minimax.dto.UChessResponse;
import com.utsab.chess_engine.minimax.service.UChessService;
import com.utsab.chess_engine.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.utsab.chess_engine.common.constants.APIConstants.API_BASE;

@RestController
@RequestMapping(API_BASE + "/minimax")
@RequiredArgsConstructor
public class UChessController extends BaseController {
    private final UChessService uChessService;

    @PostMapping
    public ResponseEntity<ApiResponse<UChessResponse>> getBestMove(@RequestBody UChessRequest uChessRequest) {
        UChessResponse uChessResponse = uChessService.getBestMove(uChessRequest);
        return successResponse(uChessResponse, "Best move calculated successfully");
    }
}
