package com.utsab.chess_engine.social.dto;

import java.util.List;

/**
 * What the app polls for while open: pending requests and challenges. Polling it also marks the user as online.
 *
 * @param outgoingChallenges the user's recent challenges, so the challenger learns when one is accepted
 */
public record SocialSummary(
        long incomingFriendRequests,
        List<ChallengeDto> incomingChallenges,
        List<ChallengeDto> outgoingChallenges
) {}
