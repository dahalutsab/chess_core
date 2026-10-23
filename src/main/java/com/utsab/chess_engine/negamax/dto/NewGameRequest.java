package com.utsab.chess_engine.negamax.dto;

import com.utsab.chess_engine.game.entity.enums.GameType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NewGameRequest(
        @NotNull GameType gameType,
        @NotBlank String botSlug,
        @NotBlank PreferredColor playerColor, // "WHITE" or "BLACK"
        String initialFen,
        String timeControl // "minutes" or "minutes|incrementSeconds", e.g. "5" or "15|10"; null or blank for untimed
) {}