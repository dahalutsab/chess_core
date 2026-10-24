package com.utsab.chess_engine.social.dto;

import java.time.LocalDateTime;

public record FriendRequestDto(Long id, PlayerSummary user, LocalDateTime createdAt) {}
