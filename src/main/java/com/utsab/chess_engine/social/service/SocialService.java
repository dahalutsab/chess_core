package com.utsab.chess_engine.social.service;

import com.utsab.chess_engine.exceptions.custom.InvalidRequestException;
import com.utsab.chess_engine.game.clock.GameClock;
import com.utsab.chess_engine.game.repository.GameRepository;
import com.utsab.chess_engine.negamax.dto.PreferredColor;
import com.utsab.chess_engine.negamax.service.UChessService;
import com.utsab.chess_engine.social.dto.*;
import com.utsab.chess_engine.social.entity.Challenge;
import com.utsab.chess_engine.social.entity.Friendship;
import com.utsab.chess_engine.social.repository.ChallengeRepository;
import com.utsab.chess_engine.social.repository.FriendshipRepository;
import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.user.repository.UserInfoRepository;
import com.utsab.chess_engine.util.file.FileUrlUtil;
import com.utsab.chess_engine.util.logged_in_user.LoggedInUserUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Friends, friend requests, presence and challenges between friends.
 *
 * @author Utsab Dahal
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialService {

    /** A user counts as online if the app polled the summary this recently */
    private static final long ONLINE_SECONDS = 90;
    /** Presence is written at most this often, so polling does not write on every request */
    private static final long TOUCH_SECONDS = 30;
    /** How long the challenger keeps seeing an answered challenge */
    private static final long OUTGOING_WINDOW_SECONDS = 600;

    private final LoggedInUserUtil loggedInUserUtil;
    private final UserInfoRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final ChallengeRepository challengeRepository;
    private final GameRepository gameRepository;
    private final UChessService gameService;

    // ---- Presence & summary ----

    @Transactional
    public SocialSummary summary() {
        UserEntity me = loggedInUserUtil.getLoggedInUser();
        touch(me);
        LocalDateTime now = LocalDateTime.now();
        List<ChallengeDto> incoming = challengeRepository
                .findByOpponentIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                        me.getId(), Challenge.Status.PENDING, now.minusSeconds(Challenge.EXPIRY_SECONDS))
                .stream().map(c -> toDto(c, me)).toList();
        List<ChallengeDto> outgoing = challengeRepository
                .findByChallengerIdAndCreatedAtAfterOrderByCreatedAtDesc(me.getId(), now.minusSeconds(OUTGOING_WINDOW_SECONDS))
                .stream().map(c -> toDto(c, me)).toList();
        long requests = me.isGuestAccount() ? 0 : friendshipRepository.countByAddresseeIdAndStatus(me.getId(), Friendship.Status.PENDING);
        return new SocialSummary(requests, incoming, outgoing);
    }

    private void touch(UserEntity user) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getLastSeenAt() == null || user.getLastSeenAt().isBefore(now.minusSeconds(TOUCH_SECONDS))) {
            user.setLastSeenAt(now);
            userRepository.save(user);
        }
    }

    // ---- Friends ----

    public List<PlayerSummary> friends() {
        UserEntity me = registeredUser();
        return friendshipRepository.findAllInvolving(me.getId()).stream()
                .filter(f -> f.getStatus() == Friendship.Status.ACCEPTED)
                .map(f -> summarize(f.other(me.getId()), "FRIEND", null))
                .sorted(Comparator.comparing(PlayerSummary::online).reversed()
                        .thenComparing(p -> p.name().toLowerCase()))
                .toList();
    }

    public FriendRequestsResponse requests() {
        UserEntity me = registeredUser();
        List<FriendRequestDto> incoming = new ArrayList<>();
        List<FriendRequestDto> outgoing = new ArrayList<>();
        for (Friendship f : friendshipRepository.findAllInvolving(me.getId())) {
            if (f.getStatus() != Friendship.Status.PENDING) continue;
            boolean sentByMe = f.getRequester().getId().equals(me.getId());
            PlayerSummary other = summarize(f.other(me.getId()), sentByMe ? "REQUEST_SENT" : "REQUEST_RECEIVED", f.getId());
            (sentByMe ? outgoing : incoming).add(new FriendRequestDto(f.getId(), other, f.getCreatedAt()));
        }
        Comparator<FriendRequestDto> newestFirst = Comparator.comparing(FriendRequestDto::createdAt).reversed();
        incoming.sort(newestFirst);
        outgoing.sort(newestFirst);
        return new FriendRequestsResponse(incoming, outgoing);
    }

    public List<PlayerSummary> search(String query) {
        UserEntity me = registeredUser();
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) return List.of();

        Map<Long, Friendship> relations = new HashMap<>();
        for (Friendship f : friendshipRepository.findAllInvolving(me.getId())) {
            relations.put(f.other(me.getId()).getId(), f);
        }
        return userRepository.search(q, me.getId(), PageRequest.of(0, 20)).stream()
                .map(user -> {
                    Friendship f = relations.get(user.getId());
                    if (f == null) return summarize(user, "NONE", null);
                    if (f.getStatus() == Friendship.Status.ACCEPTED) return summarize(user, "FRIEND", null);
                    boolean sentByMe = f.getRequester().getId().equals(me.getId());
                    return summarize(user, sentByMe ? "REQUEST_SENT" : "REQUEST_RECEIVED", f.getId());
                })
                .toList();
    }

    @Transactional
    public PlayerSummary sendRequest(Long userId) {
        UserEntity me = registeredUser();
        if (me.getId().equals(userId)) throw new InvalidRequestException("You can't add yourself as a friend");
        UserEntity other = userRepository.findById(userId)
                .filter(u -> !u.isGuestAccount())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        Optional<Friendship> existing = friendshipRepository.findBetween(me.getId(), userId);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == Friendship.Status.ACCEPTED) return summarize(other, "FRIEND", null);
            if (f.getAddressee().getId().equals(me.getId())) {
                // They already asked us: sending a request back accepts theirs
                accept(f);
                return summarize(other, "FRIEND", null);
            }
            return summarize(other, "REQUEST_SENT", f.getId());
        }

        Friendship request = friendshipRepository.save(Friendship.builder()
                .requester(me)
                .addressee(other)
                .status(Friendship.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build());
        return summarize(other, "REQUEST_SENT", request.getId());
    }

    @Transactional
    public PlayerSummary acceptRequest(Long requestId) {
        UserEntity me = registeredUser();
        Friendship f = friendshipRepository.findById(requestId)
                .filter(r -> r.getStatus() == Friendship.Status.PENDING && r.getAddressee().getId().equals(me.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));
        accept(f);
        return summarize(f.getRequester(), "FRIEND", null);
    }

    private void accept(Friendship f) {
        f.setStatus(Friendship.Status.ACCEPTED);
        f.setAcceptedAt(LocalDateTime.now());
        friendshipRepository.save(f);
    }

    /** Decline a request sent to the user, or cancel one they sent */
    @Transactional
    public void deleteRequest(Long requestId) {
        UserEntity me = registeredUser();
        Friendship f = friendshipRepository.findById(requestId)
                .filter(r -> r.getStatus() == Friendship.Status.PENDING)
                .filter(r -> r.getAddressee().getId().equals(me.getId()) || r.getRequester().getId().equals(me.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));
        friendshipRepository.delete(f);
    }

    @Transactional
    public void removeFriend(Long userId) {
        UserEntity me = registeredUser();
        Friendship f = friendshipRepository.findBetween(me.getId(), userId)
                .filter(r -> r.getStatus() == Friendship.Status.ACCEPTED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not friends with this player"));
        friendshipRepository.delete(f);
    }

    // ---- Challenges ----

    @Transactional
    public ChallengeDto challenge(NewChallengeRequest request) {
        UserEntity me = registeredUser();
        boolean friends = friendshipRepository.findBetween(me.getId(), request.opponentId())
                .map(f -> f.getStatus() == Friendship.Status.ACCEPTED)
                .orElse(false);
        if (!friends) throw new InvalidRequestException("You can only challenge your friends");
        try {
            GameClock.parse(request.timeControl());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
        UserEntity opponent = userRepository.findById(request.opponentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        // A new challenge replaces any earlier one still waiting for the same friend
        for (Challenge old : challengeRepository.findByChallengerIdAndOpponentIdAndStatus(
                me.getId(), opponent.getId(), Challenge.Status.PENDING)) {
            old.setStatus(Challenge.Status.CANCELLED);
            challengeRepository.save(old);
        }

        Challenge challenge = challengeRepository.save(Challenge.builder()
                .challenger(me)
                .opponent(opponent)
                .timeControl(request.timeControl())
                .challengerColor(request.color() != null ? request.color() : PreferredColor.RANDOM)
                .status(Challenge.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build());
        return toDto(challenge, me);
    }

    @Transactional
    public ChallengeDto acceptChallenge(UUID challengeId) {
        UserEntity me = loggedInUserUtil.getLoggedInUser();
        Challenge challenge = challengeRepository.findById(challengeId)
                .filter(c -> c.getOpponent().getId().equals(me.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found"));
        if (challenge.getStatus() != Challenge.Status.PENDING || challenge.isExpired()) {
            throw new InvalidRequestException("This challenge is no longer open");
        }
        UUID gameId = gameService.startPvpGame(challenge.getChallenger(), challenge.getOpponent(),
                challenge.getChallengerColor(), challenge.getTimeControl());
        challenge.setGameId(gameId);
        challenge.setStatus(Challenge.Status.ACCEPTED);
        challengeRepository.save(challenge);
        log.info("[SocialService] Challenge {} accepted, game {}", challengeId, gameId);
        return toDto(challenge, me);
    }

    /** Decline a challenge sent to the user, or cancel one they sent */
    @Transactional
    public ChallengeDto closeChallenge(UUID challengeId) {
        UserEntity me = loggedInUserUtil.getLoggedInUser();
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found"));
        boolean mine = challenge.getChallenger().getId().equals(me.getId());
        if (!mine && !challenge.getOpponent().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found");
        }
        if (challenge.getStatus() == Challenge.Status.PENDING) {
            challenge.setStatus(mine ? Challenge.Status.CANCELLED : Challenge.Status.DECLINED);
            challengeRepository.save(challenge);
        }
        return toDto(challenge, me);
    }

    // ---- Helpers ----

    /** The logged-in user, who must have a real account: guests have no lasting identity to befriend */
    private UserEntity registeredUser() {
        UserEntity me = loggedInUserUtil.getLoggedInUser();
        if (me.isGuestAccount()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Create an account to add friends");
        }
        return me;
    }

    private PlayerSummary summarize(UserEntity user, String relation, Long requestId) {
        LocalDateTime now = LocalDateTime.now();
        boolean online = user.getLastSeenAt() != null && user.getLastSeenAt().isAfter(now.minusSeconds(ONLINE_SECONDS));
        boolean inGame = online && gameRepository.hasGameInProgressSince(user.getId(), now.minusMinutes(30));
        return new PlayerSummary(user.getId(), user.getFullName(), FileUrlUtil.getFileUri(user.getProfileImage()),
                user.currentRating(), online, inGame, user.getLastSeenAt(), relation, requestId);
    }

    private ChallengeDto toDto(Challenge c, UserEntity viewer) {
        String status = c.isExpired() ? "EXPIRED" : c.getStatus().name();
        return new ChallengeDto(c.getId(),
                summarize(c.getChallenger(), relationLabel(c.getChallenger(), viewer), null),
                summarize(c.getOpponent(), relationLabel(c.getOpponent(), viewer), null),
                c.getTimeControl(), c.getChallengerColor().name(), status, c.getGameId(), c.getCreatedAt());
    }

    private static String relationLabel(UserEntity user, UserEntity viewer) {
        return user.getId().equals(viewer.getId()) ? "SELF" : "FRIEND";
    }
}
