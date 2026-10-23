package com.utsab.chess_engine.game.dto;

/**
 * Clock state of a timed game, as of the moment the response was created.
 *
 * @param baseTimeMs   starting time per side
 * @param incrementMs  time added after each move
 * @param whiteTimeMs  White's remaining time
 * @param blackTimeMs  Black's remaining time
 * @param running      whether the side to move's clock is running (clocks start once both sides have moved)
 *
 * @author Utsab Dahal
 * @since 2025
 */
public record ClockDto(
        long baseTimeMs,
        long incrementMs,
        long whiteTimeMs,
        long blackTimeMs,
        boolean running
) {}
