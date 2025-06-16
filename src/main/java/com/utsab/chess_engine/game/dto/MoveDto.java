package com.utsab.chess_engine.game.dto;

import java.util.List;

/**
 * Data transfer object representing a chess move, including its move number, UCI notation,
 * and optional principal variation.
 *
 * @author Utsab Dahal
 * @since 2025
 */
public record MoveDto(
        float moveNumber,
        String san,
        List<String> pv
) {
    /**
     * Constructs a MoveDto without principal variation.
     *
     * @param moveNumber the move number (e.g., 1.0 for White, 1.5 for Black)
     * @param san the move in UCI notation (e.g., "a2a3")
     */
    public MoveDto(float moveNumber, String san) {
        this(moveNumber, san, List.of());
    }
}