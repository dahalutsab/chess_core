package com.utsab.chess_engine.analysis;

import com.utsab.chess_engine.common.BaseController;
import com.utsab.chess_engine.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.utsab.chess_engine.common.constants.APIConstants.API_BASE;

@RestController
@RequestMapping(API_BASE + "/game")
@RequiredArgsConstructor
public class AnalysisController extends BaseController {

    private final AnalysisService analysisService;

    /** Engine review of a finished game. Takes a few seconds the first time; repeat requests are cached. */
    @GetMapping("/{gameId}/analysis")
    public ResponseEntity<ApiResponse<AnalysisService.GameAnalysis>> analyse(@PathVariable UUID gameId) {
        return successResponse(analysisService.analyse(gameId), "Game analysed");
    }
}
