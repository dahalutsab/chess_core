package com.utsab.chess_engine.game.controller;

import com.utsab.chess_engine.game.dto.*;
import com.utsab.chess_engine.game.service.GameService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing Player vs. Bot chess games, including creation and move operations.
 *
 * @author Utsab Dahal
 * @since 2025
 */
@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Creates a new Player vs. Bot chess game.
     *
     * @param request the request containing game details
     * @return the created game's metadata
     */
    @PostMapping
    public ResponseEntity<GameDto> createGame(@RequestBody CreateGameRequest request) {
        GameDto gameDto = gameService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(gameDto);
    }


    /**
     * Applies a player's move to a game.
     *
     * @param gameId the ID of the game
     * @param request the move request
     * @return the updated game state, including bot response
     */
    @PostMapping("/{gameId}/moves")
    public ResponseEntity<MakeMoveResponse> makeMove(@PathVariable UUID gameId,
                                                     @Valid @RequestBody MakeMoveRequest request) {
        MakeMoveResponse response = gameService.makeMove(gameId, request);
        return ResponseEntity.ok(response);
    }
    
}
