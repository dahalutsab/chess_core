package com.utsab.chess_engine.social.controller;

import com.utsab.chess_engine.common.BaseController;
import com.utsab.chess_engine.events.EventHub;
import com.utsab.chess_engine.util.logged_in_user.LoggedInUserUtil;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.utsab.chess_engine.response.ApiResponse;
import com.utsab.chess_engine.social.dto.*;
import com.utsab.chess_engine.social.service.SocialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.utsab.chess_engine.common.constants.APIConstants.API_BASE;

/**
 * Friends, friend requests and challenges.
 *
 * @author Utsab Dahal
 */
@RestController
@RequestMapping(API_BASE)
@RequiredArgsConstructor
public class SocialController extends BaseController {

    private final SocialService socialService;
    private final EventHub eventHub;
    private final LoggedInUserUtil loggedInUserUtil;

    /** Polled while the app is open: pending requests and challenges. Also marks the user as online. */
    @GetMapping("/social/summary")
    public ResponseEntity<ApiResponse<SocialSummary>> summary() {
        return successResponse(socialService.summary(), "Summary fetched successfully");
    }

    /** A stream of "summary" events whenever the user's requests or challenges change */
    @GetMapping(value = "/social/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return eventHub.subscribe(EventHub.userTopic(loggedInUserUtil.getLoggedInUser().getId()));
    }

    @GetMapping("/friends")
    public ResponseEntity<ApiResponse<List<PlayerSummary>>> friends() {
        return successResponse(socialService.friends(), "Friends fetched successfully");
    }

    @GetMapping("/friends/search")
    public ResponseEntity<ApiResponse<List<PlayerSummary>>> search(@RequestParam("q") String query) {
        return successResponse(socialService.search(query), "Players fetched successfully");
    }

    @DeleteMapping("/friends/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(@PathVariable Long userId) {
        socialService.removeFriend(userId);
        return successResponse("Friend removed");
    }

    @GetMapping("/friends/requests")
    public ResponseEntity<ApiResponse<FriendRequestsResponse>> requests() {
        return successResponse(socialService.requests(), "Friend requests fetched successfully");
    }

    /** Send a friend request to the user, or accept theirs if they already sent one */
    @PostMapping("/friends/requests/{userId}")
    public ResponseEntity<ApiResponse<PlayerSummary>> sendRequest(@PathVariable Long userId) {
        return successResponse(socialService.sendRequest(userId), "Friend request sent");
    }

    @PostMapping("/friends/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<PlayerSummary>> acceptRequest(@PathVariable Long requestId) {
        return successResponse(socialService.acceptRequest(requestId), "Friend request accepted");
    }

    /** Decline a received request or cancel a sent one */
    @DeleteMapping("/friends/requests/{requestId}")
    public ResponseEntity<ApiResponse<Void>> deleteRequest(@PathVariable Long requestId) {
        socialService.deleteRequest(requestId);
        return successResponse("Friend request removed");
    }

    @PostMapping("/challenges")
    public ResponseEntity<ApiResponse<ChallengeDto>> challenge(@Valid @RequestBody NewChallengeRequest request) {
        return successResponse(socialService.challenge(request), "Challenge sent");
    }

    @PostMapping("/challenges/{challengeId}/accept")
    public ResponseEntity<ApiResponse<ChallengeDto>> acceptChallenge(@PathVariable UUID challengeId) {
        return successResponse(socialService.acceptChallenge(challengeId), "Challenge accepted");
    }

    /** Decline a received challenge or cancel a sent one */
    @DeleteMapping("/challenges/{challengeId}")
    public ResponseEntity<ApiResponse<ChallengeDto>> closeChallenge(@PathVariable UUID challengeId) {
        return successResponse(socialService.closeChallenge(challengeId), "Challenge closed");
    }
}
