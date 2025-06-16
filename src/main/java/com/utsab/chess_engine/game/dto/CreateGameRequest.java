package com.utsab.chess_engine.game.dto;

import com.utsab.chess_engine.game.entity.enums.GameType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the request to create a new Player vs. Bot chess game, specifying player and bot IDs and player color.
 *
 * @author Utsab Dahal
 * @since 2025
 */
public record CreateGameRequest(
        @NotNull GameType gameType,
        @NotBlank Long playerId,
        @NotBlank Long botId,
        @NotBlank String playerColor, // "WHITE" or "BLACK"
        String initialFen
) {}