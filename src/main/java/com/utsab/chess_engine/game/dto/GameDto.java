package com.utsab.chess_engine.game.dto;

import com.utsab.chess_engine.game.entity.enums.GameResult;
import com.utsab.chess_engine.game.entity.enums.GameType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents the response data for a chess game, including metadata, move history, and bot move if applicable.
 *
 * @author Utsab Dahal
 * @since 2025
 */
public record GameDto(
        UUID gameId,
        GameType gameType,
        PlayerDto whitePlayer,
        PlayerDto blackPlayer,
        String initialFen,
        LocalDateTime startTime,
        LocalDateTime endTime,
        GameResult result,
        List<MoveDto> moves,
        com.utsab.chess_engine.negamax.dto.UChessResponse botMove,
        String currentTurn,
        ClockDto clock,
        String termination,
        /* The colour the requesting user plays ("WHITE" or "BLACK"), or null when they are not playing in this game */
        String yourColor
) {}

