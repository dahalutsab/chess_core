package com.utsab.chess_engine.social.dto;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * Another user as shown in friend lists and search results.
 *
 * @param relation  NONE, FRIEND, REQUEST_SENT or REQUEST_RECEIVED, from the viewing user's point of view
 * @param requestId the pending friend request between the two users, if any
 */
public record PlayerSummary(
        Long id,
        String name,
        URI avatar,
        int rating,
        boolean online,
        boolean inGame,
        LocalDateTime lastSeenAt,
        String relation,
        Long requestId
) {}
