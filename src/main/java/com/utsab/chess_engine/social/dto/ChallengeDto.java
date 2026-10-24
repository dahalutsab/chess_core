package com.utsab.chess_engine.social.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @param status PENDING, ACCEPTED, DECLINED, CANCELLED or EXPIRED
 * @param gameId the game started when the challenge was accepted
 */
public record ChallengeDto(
        UUID id,
        PlayerSummary challenger,
        PlayerSummary opponent,
        String timeControl,
        String challengerColor,
        String status,
        UUID gameId,
        LocalDateTime createdAt
) {}
