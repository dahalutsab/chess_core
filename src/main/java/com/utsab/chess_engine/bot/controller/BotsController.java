package com.utsab.chess_engine.bot.controller;

import com.utsab.chess_engine.bot.dto.BotsRequest;
import com.utsab.chess_engine.bot.dto.BotsResponse;
import com.utsab.chess_engine.bot.service.BotsService;
import com.utsab.chess_engine.common.BaseController;
import com.utsab.chess_engine.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.utsab.chess_engine.common.constants.APIConstants.API_BASE;

@RestController
@RequestMapping(API_BASE + "/bots")
@RequiredArgsConstructor
@Tag(
        name = "Bots",
        description = "APIs for managing chess bots, including creation, retrieval, and updates."
)
public class BotsController extends BaseController {

    private final BotsService botsService;

    @Operation(
            summary = "Get all bots",
            description = "Retrieves a list of all available chess bots."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<BotsResponse>>> getAllBots() {
        return successResponse(botsService.getAllBots(), "Successfully retrieved all bots.");
    }

    @Operation(
            summary = "Get bot by slug",
            description = "Retrieves a specific chess bot by its unique slug."
    )
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BotsResponse>> getBotBySlug(@PathVariable String slug) {
        return successResponse(botsService.getBotBySlug(slug), "Successfully retrieved bot by slug.");
    }

    @Operation(
            summary = "Create a new bot",
            description = "Creates a new chess bot with the provided details."
    )
    @Secured("ADMIN")
    @PostMapping
    public ResponseEntity<ApiResponse<BotsResponse>> createBot(@ModelAttribute BotsRequest botsRequest) {
        return successResponse(botsService.saveBot(botsRequest), "Successfully created a new bot.");
    }

    @Operation(
            summary = "Delete bot by slug",
            description = "Deletes a specific chess bot using its unique slug."
    )
    @Secured("ADMIN")
    @DeleteMapping("/{slug}")
    public ResponseEntity<ApiResponse<Void>> deleteBotBySlug(@PathVariable String slug) {
        botsService.deleteBotBySlug(slug);
        return successResponse(null, "Successfully deleted bot by slug.");
    }

    @Operation(
            summary = "Update bot by slug",
            description = "Updates the details of a specific chess bot using its unique slug."
    )
    @Secured("ADMIN")
    @PutMapping("/{slug}")
    public ResponseEntity<ApiResponse<BotsResponse>> updateBot(
            @PathVariable String slug, @ModelAttribute BotsRequest botsRequest) {
        return successResponse(botsService.updateBot(slug, botsRequest), "Successfully updated bot by slug.");
    }
}
