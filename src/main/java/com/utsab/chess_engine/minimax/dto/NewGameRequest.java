package com.utsab.chess_engine.minimax.dto;

import com.utsab.chess_engine.game.entity.enums.GameType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NewGameRequest(
        @NotNull GameType gameType,
        @NotBlank String botSlug,
        @NotBlank PreferredColor playerColor, // "WHITE" or "BLACK"
        String initialFen
) {}