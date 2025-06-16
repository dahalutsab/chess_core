package com.utsab.chess_engine.game.dto;

import java.util.List;

/**
 * Represents the response containing valid moveEntities for a game position.
 *
 * @author Utsab Dahal
 * @since 2025
 */
public record ValidMovesResponse(
        String gameId,
        String currentFen,
        List<String> validMoves
) {}