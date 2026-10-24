package com.utsab.chess_engine.social.dto;

import java.util.List;

public record FriendRequestsResponse(List<FriendRequestDto> incoming, List<FriendRequestDto> outgoing) {}
