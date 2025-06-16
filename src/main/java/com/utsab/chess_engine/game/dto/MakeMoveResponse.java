package com.utsab.chess_engine.game.dto;

import com.utsab.chess_engine.game.entity.enums.GameResult;

import java.util.UUID;

/**
 * Represents the response after making a move in a chess game, including bot response if applicable.
 *
 * @author Utsab Dahal
 * @since 2025
 */
public record MakeMoveResponse(
        UUID gameId,
        MoveDto move,
        String newFen,
        GameResult result,
        MoveDto botMove
) {}