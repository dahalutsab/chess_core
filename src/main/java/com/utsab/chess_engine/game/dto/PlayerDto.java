package com.utsab.chess_engine.game.dto;

import java.net.URI;

/**
 * @param rating       the player's rating when the game started
 * @param ratingChange how much the rating changed when the game ended; null while the game is in progress
 */
public record PlayerDto(
        Long id,
        String type, // "USER" or "BOT"
        String name,
        Integer rating,
        Integer ratingChange,
        URI avatar
) {}
