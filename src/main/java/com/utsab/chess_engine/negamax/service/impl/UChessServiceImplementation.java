package com.utsab.chess_engine.negamax.service.impl;

import com.utsab.chess_engine.bot.entity.Bots;
import com.utsab.chess_engine.bot.repository.BotRepository;
import com.utsab.chess_engine.common.entity.Player;
import com.utsab.chess_engine.core.UChessEngineUCI;
import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.Move;
import com.utsab.chess_engine.core.evaluation.Result;
import com.utsab.chess_engine.core.search.SearchResult;
import com.utsab.chess_engine.core.utils.notation.FEN;
import com.utsab.chess_engine.core.utils.notation.Notation;
import com.utsab.chess_engine.core.utils.notation.SAN;
import com.utsab.chess_engine.exceptions.custom.InvalidRequestException;
import com.utsab.chess_engine.game.clock.GameClock;
import com.utsab.chess_engine.game.dto.GameDto;
import com.utsab.chess_engine.game.dto.MoveDto;
import com.utsab.chess_engine.game.dto.PlayerDto;
import com.utsab.chess_engine.game.dto.history.BotResponse;
import com.utsab.chess_engine.game.dto.history.GameStatsResponse;
import com.utsab.chess_engine.game.dto.history.GamesResponse;
import com.utsab.chess_engine.game.dto.history.HistoryResponse;
import com.utsab.chess_engine.game.entity.Game;
import com.utsab.chess_engine.game.entity.enums.GameResult;
import com.utsab.chess_engine.game.entity.enums.GameType;
import com.utsab.chess_engine.game.repository.GameRepository;
import com.utsab.chess_engine.negamax.dto.NewGameRequest;
import com.utsab.chess_engine.negamax.dto.PreferredColor;
import com.utsab.chess_engine.negamax.dto.UChessRequest;
import com.utsab.chess_engine.negamax.dto.UChessResponse;
import com.utsab.chess_engine.negamax.service.UChessService;
import com.utsab.chess_engine.rating.RatingService;
import com.utsab.chess_engine.events.EventHub;
import com.utsab.chess_engine.core.evaluation.Score;
import com.utsab.chess_engine.util.file.FileUrlUtil;
import org.hibernate.Hibernate;
import org.springframework.data.domain.PageRequest;
import com.utsab.chess_engine.move.entity.MoveEntity;
import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.util.logged_in_user.LoggedInUserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of the UChessService interface for handling UCI-compatible chess game operations.
 * Manages game initialization, move calculation, and game state retrieval.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UChessServiceImplementation implements UChessService {

    private final GameRepository gameRepository;
    private final BotRepository botRepository;
    private final LoggedInUserUtil loggedInUserUtil;
    private final RatingService ratingService;
    private final EventHub eventHub;
    private final TransactionTemplate transactionTemplate;

    /**
     * Starts a new chess game based on the provided request.
     * Initializes the game state, assigns players, and optionally computes an initial bot move.
     *
     * @param request the request containing game type, bot slug, player color, and optional initial FEN
     * @return a DTO containing the initialized game state
     * @throws InvalidRequestException if the bot slug is invalid
     * @author Utsab Dahal
     */
    @Override
    public GameDto startNewGame(NewGameRequest request) {
        synchronized (UChessEngineUCI.ENGINE) {
            return startNewGameLocked(request);
        }
    }

    private GameDto startNewGameLocked(NewGameRequest request) {
        UChessEngineUCI.ENGINE.gameOver();
        UChessEngineUCI.ENGINE.newGame();

        UserEntity user = loggedInUserUtil.getLoggedInUser();
        Bots bot = botRepository.findBySlug(request.botSlug())
                .orElseThrow(() -> new InvalidRequestException("Bot not found: " + request.botSlug()));

        String initialFen = Objects.requireNonNullElse(request.initialFen(), FEN.STARTING_POSITION);
        GameClock.TimeControl timeControl;
        try {
            timeControl = GameClock.parse(request.timeControl());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }

        Player whitePlayer;
        Player blackPlayer;
        PreferredColor color = request.playerColor();
        if (color == PreferredColor.RANDOM) {
            boolean userIsWhite = Math.random() < 0.5;
            whitePlayer = userIsWhite ? user : bot;
            blackPlayer = userIsWhite ? bot : user;
            color = userIsWhite ? PreferredColor.WHITE : PreferredColor.BLACK;
        } else {
            whitePlayer = color == PreferredColor.WHITE ? user : bot;
            blackPlayer = color == PreferredColor.BLACK ? user : bot;
        }

        // Validate player types
        if (whitePlayer == null || blackPlayer == null) {
            throw new InvalidRequestException("Invalid player types assigned");
        }

        Game game = Game.builder()
                .whitePlayer(whitePlayer)
                .blackPlayer(blackPlayer)
                .initialFen(initialFen)
                .startTime(LocalDateTime.now())
                .result(GameResult.IN_PROGRESS)
                .gameType(GameType.PVB)
                .moveEntities(new ArrayList<>())
                .whiteRatingBefore(RatingService.ratingOf(whitePlayer))
                .blackRatingBefore(RatingService.ratingOf(blackPlayer))
                .build();
        GameClock.start(game, timeControl, System.currentTimeMillis());

        game = gameRepository.save(game);

        UChessResponse botMove = null;
        if (color == PreferredColor.BLACK) {
            UChessRequest uChessRequest = new UChessRequest(game.getId(), null);
            botMove = getBestMove(uChessRequest);
        }

        // Reload, so that the bot's opening move and the clock state are included
        game = gameRepository.findById(game.getId()).orElseThrow();
        UChessEngineUCI.ENGINE.setPosition(initialFen,
                game.getMoveEntities().stream().map(MoveEntity::getUci).map(Notation::fromCombinedNotation).toList());
        return toGameDto(game, UChessEngineUCI.ENGINE.getBoard(), botMove);
    }

    @Override
    public UChessResponse getBestMove(UChessRequest uChessRequest) {
        // The engine is a single shared instance, so requests from different games must not interleave
        synchronized (UChessEngineUCI.ENGINE) {
            return getBestMoveLocked(uChessRequest);
        }
    }

    private UChessResponse getBestMoveLocked(UChessRequest uChessRequest) {
        Game game = gameRepository.findById(uChessRequest.getGameId())
                .orElseThrow(() -> new InvalidRequestException("Game not found: " + uChessRequest.getGameId()));

        if (game.getResult() != null && game.getResult().isFinished()) {
            String reason = GameClock.TIMEOUT.equals(game.getTermination())
                    ? game.getResult().getNotation() + " on time"
                    : game.getResult().getDescription();
            throw new InvalidRequestException("Game is already over: " + reason);
        }

        Bots bot = botOf(game);
        int maxDepth = bot != null ? bot.getMaxDepth() : 200;
        UChessEngineUCI.ENGINE.getConfig().setMaxDepth(maxDepth);
        log.debug("Max depth set to: {}", maxDepth);

        List<MoveEntity> moves = new ArrayList<>(game.getMoveEntities());
        List<Move> moveList = moves.stream().map(MoveEntity::getUci).map(Notation::fromCombinedNotation).toList();

        String fen = game.getInitialFen();
        UUID gameId = uChessRequest.getGameId();

        UChessEngineUCI.ENGINE.setPosition(fen, moveList);
        Board board = UChessEngineUCI.ENGINE.getBoard();

        // A move may only be submitted on the user's turn, and the bot may only be asked to move (no move) on its own
        // turn; otherwise a repeated or stale request would make the engine play the user's side.
        UserEntity user = loggedInUserUtil.getLoggedInUser();
        boolean userIsWhite = game.getWhitePlayer().getId().equals(user.getId());
        boolean userIsBlack = game.getBlackPlayer().getId().equals(user.getId());
        if (!userIsWhite && !userIsBlack) {
            throw new InvalidRequestException("You are not playing in this game");
        }
        boolean botToMove = "BOT".equals(getPlayerType(board.isWhiteToMove() ? game.getWhitePlayer() : game.getBlackPlayer()));
        if (uChessRequest.getMove() == null && !botToMove) {
            throw new InvalidRequestException("It is not the bot's turn to move");
        }
        if (uChessRequest.getMove() != null && !(board.isWhiteToMove() ? userIsWhite : userIsBlack)) {
            throw new InvalidRequestException("It is not your turn to move");
        }

        // When the player's move counts as made: on arrival, less the network delay (see GameClock.moveTime), so the
        // player isn't charged for the server's own work before it got to this point
        long now = uChessRequest.getMove() != null
                ? GameClock.moveTime(game, uChessRequest.getReceivedAt(), uChessRequest.getThinkTimeMs(), System.currentTimeMillis())
                : System.currentTimeMillis();

        // If the side to move has run out of time, the game is over before this request can play anything
        if (GameClock.isFlagged(game, board.isWhiteToMove(), now)) {
            finishOnTime(game, board, board.isWhiteToMove());
            return new UChessResponse(gameId, fen, null, null, game.getResult().name(),
                    GameClock.snapshot(game, board.isWhiteToMove(), now), game.getTermination());
        }

        if (uChessRequest.getMove() != null) {
            List<Move> validMoves = UChessEngineUCI.ENGINE.getMoveGenerator().generateMoves(board);
            if (validMoves.stream().noneMatch(m -> Notation.toNotation(m).equals(uChessRequest.getMove()))) {
                throw new InvalidRequestException("Invalid move requested: " + uChessRequest.getMove());
            }
            answerDrawOfferByMoving(game, board.isWhiteToMove());
            GameClock.recordMove(game, board.isWhiteToMove(), now);
            game.getMoveEntities().add(MoveEntity.builder()
                    .game(game)
                    .moveNumber(calculateMoveNumber(game.getMoveEntities().size() + 1))
                    .uci(uChessRequest.getMove())
                    .build());
            moveList = new ArrayList<>(moveList);
            moveList.add(Notation.fromCombinedNotation(uChessRequest.getMove()));
            UChessEngineUCI.ENGINE.setPosition(fen, moveList);
            board = UChessEngineUCI.ENGINE.getBoard();
        }

        // The player's move may already have ended the game, in which case there is nothing for the bot to search
        String gameResult = adjudicate(board);
        String bestMove = null;
        String bestMoveSan = null;

        // Against a friend there is no reply to compute: the opponent moves through their own request
        boolean botReplies = "BOT".equals(getPlayerType(board.isWhiteToMove() ? game.getWhitePlayer() : game.getBlackPlayer()));
        if (gameResult == null && botReplies) {
            // In a timed game the bot spends less than its usual second per move when its clock runs low. Its clock
            // starts now, as it starts thinking.
            boolean botIsWhite = board.isWhiteToMove();
            long thinkStart = System.currentTimeMillis();
            GameClock.restart(game, thinkStart);
            SearchResult result = UChessEngineUCI.ENGINE.thinkSync(GameClock.botThinkTime(game, botIsWhite, thinkStart));

            if (!GameClock.recordMove(game, botIsWhite, System.currentTimeMillis())) {
                // The bot ran out of time while thinking, so its move doesn't count
                finishOnTime(game, board, botIsWhite);
                return new UChessResponse(gameId, fen, null, null, game.getResult().name(),
                        GameClock.snapshot(game, botIsWhite, System.currentTimeMillis()), game.getTermination());
            }

            answerDrawOfferByMoving(game, botIsWhite);
            bestMove = Notation.toNotation(result.move());
            bestMoveSan = SAN.toSan(result.move(), board);
            // Use the engine's own move object: it carries the special-move flags (castling, en passant, ...)
            board.makeMove(result.move());
            gameResult = adjudicate(board);

            game.getMoveEntities().add(MoveEntity.builder()
                    .game(game)
                    .moveNumber(calculateMoveNumber(game.getMoveEntities().size() + 1))
                    .uci(bestMove)
                    .build());
        }

        if (gameResult != null) {
            game.setResult(GameResult.valueOf(gameResult));
            finish(game);
        }
        // The next player's clock starts once the server's work is done, not while it was still saving
        if (gameResult == null) GameClock.restart(game, System.currentTimeMillis());
        gameRepository.save(game);
        notifyPlayers(game);

        return new UChessResponse(gameId, fen, bestMove, bestMoveSan, gameResult,
                GameClock.snapshot(game, board.isWhiteToMove(), System.currentTimeMillis()), game.getTermination());
    }

    /**
     * End the game because the given side ran out of time, and save it.
     */
    private void finishOnTime(Game game, Board board, boolean flaggedWhite) {
        GameClock.flag(game, board, flaggedWhite);
        finish(game);
        gameRepository.save(game);
        notifyPlayers(game);
    }

    /** Tell everyone watching the game that it changed; they fetch the new state */
    private void notifyPlayers(Game game) {
        eventHub.publish(EventHub.gameTopic(game.getId()), "update",
                Map.of("ply", game.getMoveEntities().size(), "result", game.getResult().name()));
    }

    /** Moving answers a draw offer from the other side: as on most chess sites, it declines it */
    private static void answerDrawOfferByMoving(Game game, boolean moverIsWhite) {
        String mover = moverIsWhite ? "WHITE" : "BLACK";
        if (game.getDrawOfferedBy() != null && !game.getDrawOfferedBy().equals(mover)) {
            game.setDrawOfferedBy(null);
        }
    }

    /**
     * Bookkeeping for a game whose result has just been set: records the end time and updates the players' ratings.
     */
    private void finish(Game game) {
        if (game.getEndTime() == null) game.setEndTime(LocalDateTime.now());
        game.setDrawOfferedBy(null);
        ratingService.rate(game);
    }

    @Override
    public GameDto resign(UUID gameId) {
        synchronized (UChessEngineUCI.ENGINE) {
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new InvalidRequestException("Game not found: " + gameId));
            UserEntity user = loggedInUserUtil.getLoggedInUser();
            boolean userIsWhite = game.getWhitePlayer().getId().equals(user.getId());
            if (!userIsWhite && !game.getBlackPlayer().getId().equals(user.getId())) {
                throw new InvalidRequestException("You are not playing in this game");
            }

            Board board = setUpPosition(game);
            long now = System.currentTimeMillis();
            if (GameClock.isFlagged(game, board.isWhiteToMove(), now)) {
                // Lost on time before resigning
                finishOnTime(game, board, board.isWhiteToMove());
                return toGameDto(game, board, null);
            }
            if (game.getResult().isFinished()) {
                throw new InvalidRequestException("Game is already over");
            }

            GameClock.stop(game, board.isWhiteToMove(), now);
            game.setResult(userIsWhite ? GameResult.BLACK_WIN : GameResult.WHITE_WIN);
            game.setTermination(Game.RESIGNATION);
            finish(game);
            gameRepository.save(game);
            notifyPlayers(game);
            return toGameDto(game, board, null);
        }
    }

    @Override
    public UUID startPvpGame(UserEntity challenger, UserEntity opponent, PreferredColor challengerColor, String timeControl) {
        GameClock.TimeControl clock;
        try {
            clock = GameClock.parse(timeControl);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
        boolean challengerIsWhite = challengerColor == PreferredColor.RANDOM
                ? Math.random() < 0.5
                : challengerColor == PreferredColor.WHITE;
        UserEntity white = challengerIsWhite ? challenger : opponent;
        UserEntity black = challengerIsWhite ? opponent : challenger;

        Game game = Game.builder()
                .whitePlayer(white)
                .blackPlayer(black)
                .initialFen(FEN.STARTING_POSITION)
                .startTime(LocalDateTime.now())
                .result(GameResult.IN_PROGRESS)
                .gameType(GameType.PVP)
                .moveEntities(new ArrayList<>())
                .whiteRatingBefore(white.currentRating())
                .blackRatingBefore(black.currentRating())
                .build();
        GameClock.start(game, clock, System.currentTimeMillis());
        return gameRepository.save(game).getId();
    }

    @Override
    public List<RatingPoint> getRatingHistory() {
        Long userId = loggedInUserUtil.getLoggedInUser().getId();
        // Newest first from the query; the points go oldest first
        List<GameRepository.RatedGame> games = new ArrayList<>(gameRepository.findRatedGames(userId, PageRequest.of(0, 200)));
        java.util.Collections.reverse(games);
        List<RatingPoint> points = new ArrayList<>();
        for (GameRepository.RatedGame game : games) {
            boolean white = game.whitePlayerId().equals(userId);
            Integer before = white ? game.whiteRatingBefore() : game.blackRatingBefore();
            Integer change = white ? game.whiteRatingChange() : game.blackRatingChange();
            if (before == null || change == null) continue;
            if (points.isEmpty()) points.add(new RatingPoint(game.startTime().toString(), before));
            points.add(new RatingPoint(game.endTime().toString(), before + change));
        }
        return points;
    }

    // ---- Draw offers ----

    /** Bots accept a draw only after this many plies, and only when they are not better */
    private static final int BOT_DRAW_MIN_PLIES = 30;
    private static final int BOT_DRAW_MAX_ADVANTAGE_CP = 30;
    private static final int BOT_DRAW_THINK_MS = 300;

    /**
     * Offer a draw, or accept the opponent's offer if they made one. A bot answers straight away: it accepts if the
     * game has gone on a while and it doesn't think it is better.
     */
    @Override
    public GameDto offerDraw(UUID gameId) {
        synchronized (UChessEngineUCI.ENGINE) {
            Game game = playableGame(gameId);
            String me = colourOf(game, loggedInUserUtil.getLoggedInUser());
            String opponent = "WHITE".equals(me) ? "BLACK" : "WHITE";
            Board board = setUpPosition(game);

            if (opponent.equals(game.getDrawOfferedBy())) {
                agreeDraw(game, board);
                return toGameDto(game, board, null);
            }
            if (me.equals(game.getDrawOfferedBy())) {
                return toGameDto(game, board, null);
            }

            Player opponentPlayer = "WHITE".equals(opponent) ? game.getWhitePlayer() : game.getBlackPlayer();
            if ("BOT".equals(getPlayerType(opponentPlayer))) {
                if (botAcceptsDraw(game, board, "WHITE".equals(opponent))) {
                    agreeDraw(game, board);
                } else {
                    throw new InvalidRequestException(getPlayerName(opponentPlayer) + " declined your draw offer");
                }
                return toGameDto(game, setUpPosition(game), null);
            }

            game.setDrawOfferedBy(me);
            gameRepository.save(game);
            notifyPlayers(game);
            return toGameDto(game, board, null);
        }
    }

    @Override
    public GameDto declineDraw(UUID gameId) {
        synchronized (UChessEngineUCI.ENGINE) {
            Game game = playableGame(gameId);
            String me = colourOf(game, loggedInUserUtil.getLoggedInUser());
            if (game.getDrawOfferedBy() != null && !game.getDrawOfferedBy().equals(me)) {
                game.setDrawOfferedBy(null);
                gameRepository.save(game);
                notifyPlayers(game);
            }
            return toGameDto(game, setUpPosition(game), null);
        }
    }

    private void agreeDraw(Game game, Board board) {
        GameClock.stop(game, board.isWhiteToMove(), System.currentTimeMillis());
        game.setResult(GameResult.DRAW_BY_AGREEMENT);
        finish(game);
        gameRepository.save(game);
        notifyPlayers(game);
    }

    private boolean botAcceptsDraw(Game game, Board board, boolean botIsWhite) {
        if (game.getMoveEntities().size() < BOT_DRAW_MIN_PLIES) return false;
        // Search directly rather than through thinkSync, which would answer from the opening book without an eval
        int eval = UChessEngineUCI.ENGINE.think(BOT_DRAW_THINK_MS).eval();
        int botEval = board.isWhiteToMove() == botIsWhite ? eval : -eval;
        return !Score.isMateScore(botEval) && botEval <= BOT_DRAW_MAX_ADVANTAGE_CP;
    }

    /** The game, which must be in progress and include the logged-in user */
    private Game playableGame(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new InvalidRequestException("Game not found: " + gameId));
        colourOf(game, loggedInUserUtil.getLoggedInUser());
        if (game.getResult().isFinished()) throw new InvalidRequestException("Game is already over");
        return game;
    }

    private static String colourOf(Game game, UserEntity user) {
        if (game.getWhitePlayer().getId().equals(user.getId())) return "WHITE";
        if (game.getBlackPlayer().getId().equals(user.getId())) return "BLACK";
        throw new InvalidRequestException("You are not playing in this game");
    }

    // ---- Abandoned games ----

    /** A game neither side moved in is aborted after this long */
    private static final Duration ABORT_AFTER = Duration.ofMinutes(10);
    /** An untimed game against a friend is lost by the player to move after this long without a move */
    private static final Duration PVP_ABANDON_AFTER = Duration.ofHours(24);
    /** An untimed game against a bot waits longer, as only the user's own game is held up */
    private static final Duration PVB_ABANDON_AFTER = Duration.ofDays(7);

    /**
     * End games that were left unfinished: timed games whose player to move ran out of time, games where a side
     * never moved, and untimed games nobody has moved in for a long time.
     *
     * @return how many games were ended
     */
    @Override
    public int sweepAbandonedGames() {
        LocalDateTime now = LocalDateTime.now();
        int ended = 0;
        List<UUID> candidates = gameRepository.findSweepCandidates(
                now.minus(ABORT_AFTER), now.minus(PVP_ABANDON_AFTER), now.minus(PVB_ABANDON_AFTER));
        for (UUID gameId : candidates) {
            // The scheduler has no request-scoped session, so each game is handled in its own transaction
            Boolean endedThis = transactionTemplate.execute(status -> sweepGame(gameId, now));
            if (Boolean.TRUE.equals(endedThis)) ended++;
        }
        return ended;
    }

    private boolean sweepGame(UUID gameId, LocalDateTime now) {
        synchronized (UChessEngineUCI.ENGINE) {
            Game game = gameRepository.findById(gameId).orElse(null);
            if (game == null || game.getResult().isFinished()) return false;
            Board board = setUpPosition(game);
            boolean whiteToMove = board.isWhiteToMove();
            LocalDateTime lastActivity = lastActivity(game);

            if (GameClock.isFlagged(game, whiteToMove, System.currentTimeMillis())) {
                finishOnTime(game, board, whiteToMove);
            } else if (game.getMoveEntities().size() < 2 && lastActivity.isBefore(now.minus(ABORT_AFTER))) {
                endAbandoned(game, whiteToMove, Game.ABORTED);
            } else if (!GameClock.isTimed(game) && lastActivity.isBefore(now.minus(
                    game.getGameType() == GameType.PVP ? PVP_ABANDON_AFTER : PVB_ABANDON_AFTER))) {
                endAbandoned(game, whiteToMove, Game.ABANDONED);
            } else {
                return false;
            }
            log.info("[UChessService] Ended abandoned game {}: {} {}", gameId, game.getResult(), game.getTermination());
            return true;
        }
    }

    /**
     * When the game last changed. Adding a move doesn't touch the game row itself, so the newest move counts too.
     */
    private static LocalDateTime lastActivity(Game game) {
        LocalDateTime latest = Objects.requireNonNullElse(game.getLastModifiedDate(), game.getStartTime());
        for (MoveEntity move : game.getMoveEntities()) {
            if (move.getCreatedDate() != null && move.getCreatedDate().isAfter(latest)) latest = move.getCreatedDate();
        }
        return latest;
    }

    private void endAbandoned(Game game, boolean whiteToMove, String termination) {
        game.setResult(whiteToMove ? GameResult.BLACK_WIN : GameResult.WHITE_WIN);
        game.setTermination(termination);
        finish(game);
        gameRepository.save(game);
        notifyPlayers(game);
    }

    /** Load the game's position into the engine and return the board */
    private Board setUpPosition(Game game) {
        List<Move> moves = game.getMoveEntities().stream().map(MoveEntity::getUci).map(Notation::fromCombinedNotation).toList();
        UChessEngineUCI.ENGINE.setPosition(game.getInitialFen(), moves);
        return UChessEngineUCI.ENGINE.getBoard();
    }

    private GameDto toGameDto(Game game, Board board, UChessResponse botMove) {
        String yourColor = null;
        try {
            Long userId = loggedInUserUtil.getLoggedInUser().getId();
            if (game.getWhitePlayer().getId().equals(userId)) yourColor = "WHITE";
            else if (game.getBlackPlayer().getId().equals(userId)) yourColor = "BLACK";
        } catch (RuntimeException e) {
            // Not signed in: nobody's colour
        }
        return new GameDto(
                game.getId(),
                game.getGameType(),
                toPlayerDto(game.getWhitePlayer(), game.getWhiteRatingBefore(), game.getWhiteRatingChange()),
                toPlayerDto(game.getBlackPlayer(), game.getBlackRatingBefore(), game.getBlackRatingChange()),
                game.getInitialFen(),
                game.getStartTime(),
                game.getEndTime(),
                game.getResult(),
                game.getMoveEntities().stream().map(m -> new MoveDto(m.getMoveNumber(), m.getUci())).toList(),
                botMove,
                board.isWhiteToMove() ? "WHITE" : "BLACK",
                GameClock.snapshot(game, board.isWhiteToMove(), System.currentTimeMillis()),
                game.getTermination(),
                yourColor,
                game.getDrawOfferedBy()
        );
    }

    private PlayerDto toPlayerDto(Player player, Integer ratingBefore, Integer ratingChange) {
        // Games from before ratings existed have no recorded rating: show the player's current one
        int rating = ratingBefore != null ? ratingBefore : RatingService.ratingOf(player);
        return new PlayerDto(player.getId(), getPlayerType(player), getPlayerName(player), rating, ratingChange, avatarOf(player));
    }

    private static URI avatarOf(Player player) {
        Object real = Hibernate.unproxy(player);
        if (real instanceof UserEntity user) return FileUrlUtil.getFileUri(user.getProfileImage());
        if (real instanceof Bots bot) return FileUrlUtil.getFileUri(bot.getBotImageUrl());
        return null;
    }

    /**
     * Determine whether the game is over in the given position, using the actual rules of chess (e.g. threefold, not
     * the search's twofold repetition).
     *
     * @return the name of the matching {@link GameResult}, or null if the game continues
     */
    private String adjudicate(Board board) {
        if (UChessEngineUCI.ENGINE.getMoveGenerator().generateMoves(board).isEmpty()) {
            if (UChessEngineUCI.ENGINE.getMoveGenerator().isCheck(board, board.isWhiteToMove())) {
                return board.isWhiteToMove() ? GameResult.BLACK_WIN.name() : GameResult.WHITE_WIN.name();
            }
            return GameResult.DRAW_BY_STALEMATE.name();
        }
        if (Result.isThreefoldRepetition(board)) return GameResult.DRAW_BY_REPETITION.name();
        if (Result.isFiftyMoveRule(board)) return GameResult.DRAW_BY_FIFTY_MOVE_RULE.name();
        if (Result.isInsufficientMaterial(board)) return GameResult.DRAW_BY_INSUFFICIENT_MATERIAL.name();
        return null;
    }

    @Override
    public GameDto getGameInfo(UUID gameId) {
        synchronized (UChessEngineUCI.ENGINE) {
            return getGameInfoLocked(gameId);
        }
    }

    private GameDto getGameInfoLocked(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new InvalidRequestException("Game not found: " + gameId));

        List<String> uciMoves = game.getMoveEntities().stream().map(MoveEntity::getUci).toList();
        UChessEngineUCI.ENGINE.setPosition(game.getInitialFen(), uciMoves.stream().map(Notation::fromCombinedNotation).toList());
        Board board = UChessEngineUCI.ENGINE.getBoard();

        // Time-outs are detected lazily: a game whose side to move ran out of time is finished when it is next read
        if (GameClock.isFlagged(game, board.isWhiteToMove(), System.currentTimeMillis())) {
            finishOnTime(game, board, board.isWhiteToMove());
        }
        return toGameDto(game, board, null);
    }

    @Override
    public HistoryResponse getGameHistory(Pageable pageable) {
        UserEntity currentUser = loggedInUserUtil.getLoggedInUser();
        Long userId = currentUser.getId();

        Page<Game> games = gameRepository.findByWhitePlayerIdOrBlackPlayerIdOrderByStartTimeDesc(userId, userId, pageable);

        Page<GamesResponse> gamesResponses = games.map(game -> {
            boolean white = game.getWhitePlayer().getId().equals(userId);
            Player opponent = white ? game.getBlackPlayer() : game.getWhitePlayer();

            BotResponse botResponse = null;
            if (Hibernate.unproxy(opponent) instanceof Bots bot) {
                botResponse = new BotResponse(bot);
            }

            Integer before = white ? game.getWhiteRatingBefore() : game.getBlackRatingBefore();
            Integer change = white ? game.getWhiteRatingChange() : game.getBlackRatingChange();
            Integer opponentRating = white ? game.getBlackRatingBefore() : game.getWhiteRatingBefore();

            return GamesResponse.builder()
                    .gameId(game.getId().toString())
                    .gameType(game.getGameType().name())
                    .whitePlayer(getPlayerName(game.getWhitePlayer()))
                    .blackPlayer(getPlayerName(game.getBlackPlayer()))
                    .startTime(game.getStartTime().toString())
                    .endTime(game.getEndTime() != null ? game.getEndTime().toString() : null)
                    .result(game.getResult().name())
                    .termination(game.getTermination())
                    .moveCount(game.getMoveEntities().size())
                    .playerColor(white ? "WHITE" : "BLACK")
                    .bot(botResponse)
                    .opponentName(getPlayerName(opponent))
                    .opponentAvatar(avatarOf(opponent))
                    .opponentRating(opponentRating != null ? opponentRating : RatingService.ratingOf(opponent))
                    .ratingChange(change)
                    .rating(before != null ? before + (change != null ? change : 0) : null)
                    .build();
        });

        // Statistics cover all of the user's games, not just this page
        int wins = (int) gameRepository.countWins(userId);
        int losses = (int) gameRepository.countLosses(userId);
        int draws = (int) gameRepository.countWithResults(userId, GameResult.draws());
        int played = (int) gameRepository.countCounted(userId);
        int rating = currentUser.currentRating();
        int peak = getRatingHistory().stream().mapToInt(RatingPoint::rating).max().orElse(rating);

        GameStatsResponse stats = new GameStatsResponse(
                played, wins, losses, draws, rating, Math.max(peak, rating), currentUser.ratedGameCount());

        return new HistoryResponse(stats, gamesResponses);
    }

    /**
     * "USER" or "BOT". Players are loaded once per request and their subclass read directly, rather than asking the
     * database each time.
     */
    private static String getPlayerType(Player player) {
        return Hibernate.unproxy(player) instanceof Bots ? "BOT" : "USER";
    }

    /** The user's full name or the bot's name */
    private static String getPlayerName(Player player) {
        Object real = Hibernate.unproxy(player);
        if (real instanceof UserEntity user && user.getFullName() != null) return user.getFullName();
        if (real instanceof Bots bot && bot.getBotName() != null) return bot.getBotName();
        return "Player-" + player.getId();
    }

    /** The bot playing in the game, if any */
    private static Bots botOf(Game game) {
        if (Hibernate.unproxy(game.getWhitePlayer()) instanceof Bots bot) return bot;
        if (Hibernate.unproxy(game.getBlackPlayer()) instanceof Bots bot) return bot;
        return null;
    }

    /**
     * Calculates the move number based on the total number of moves played.
     * White's moves are whole numbers (e.g., 1.0, 2.0), and Black's moves are half numbers (e.g., 1.5, 2.5).
     *
     * @param totalMoves the total number of moves played in the game
     * @return the move number as a float
     */
    private float calculateMoveNumber(int totalMoves) {
        if (totalMoves % 2 == 1) {
            return (totalMoves + 1) / 2.0f;
        } else {
            return totalMoves / 2.0f + 0.5f;
        }
    }
}