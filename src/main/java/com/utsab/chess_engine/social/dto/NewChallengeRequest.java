package com.utsab.chess_engine.social.dto;

import com.utsab.chess_engine.negamax.dto.PreferredColor;
import jakarta.validation.constraints.NotNull;

/**
 * @param timeControl "minutes" or "minutes|incrementSeconds"; null or blank for untimed
 * @param color       the colour the challenger wants to play
 */
public record NewChallengeRequest(@NotNull Long opponentId, String timeControl, PreferredColor color) {}
