package com.utsab.chess_engine.game.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents the request to make a move in a chess game.
 *
 * @author Utsab Dahal
 * @since 2025
 */
public record MakeMoveRequest(
        @NotBlank String san
) {}