package com.utsab.chess_engine.game.dto;

/**
 * Represents a player in a chess game, either a user or a bot.
 *
 * @author Utsab Dahal
 * @since 2025
 */
public record PlayerDto(
        Long id,
        String type, // "USER" or "BOT"
        String name
) {}
