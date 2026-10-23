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
import com.utsab.chess_engine.game.repository.GameRepository;
import com.utsab.chess_engine.negamax.dto.NewGameRequest;
import com.utsab.chess_engine.negamax.dto.PreferredColor;
import com.utsab.chess_engine.negamax.dto.UChessRequest;
import com.utsab.chess_engine.negamax.dto.UChessResponse;
import com.utsab.chess_engine.negamax.service.UChessService;
import com.utsab.chess_engine.move.entity.MoveEntity;
import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.util.logged_in_user.LoggedInUserUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
    @PersistenceContext
    private final EntityManager entityManager;

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
                .gameType(request.gameType())
                .moveEntities(new ArrayList<>())
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

        int maxDepth;
        if ("BOT".equals(getPlayerType(game.getBlackPlayer()))) {
            Bots bot = botRepository.findById(game.getBlackPlayer().getId())
                    .orElseThrow(() -> new InvalidRequestException("Bot not found for player id: " + game.getBlackPlayer().getId()));
            maxDepth = bot.getMaxDepth();
        } else if ("BOT".equals(getPlayerType(game.getWhitePlayer()))) {
            Bots bot = botRepository.findById(game.getWhitePlayer().getId())
                    .orElseThrow(() -> new InvalidRequestException("Bot not found for player id: " + game.getWhitePlayer().getId()));
            maxDepth = bot.getMaxDepth();
        } else {
            maxDepth = 200;
        }
        UChessEngineUCI.ENGINE.getConfig().setMaxDepth(maxDepth);
        log.info("Max depth set to: {}", UChessEngineUCI.ENGINE.getConfig().getMaxDepth());

        List<MoveEntity> moves = new ArrayList<>(game.getMoveEntities());
        List<Move> moveList = moves.stream().map(MoveEntity::getUci).map(Notation::fromCombinedNotation).toList();

        String fen = game.getInitialFen();
        UUID gameId = uChessRequest.getGameId();

        UChessEngineUCI.ENGINE.setPosition(fen, moveList);
        Board board = UChessEngineUCI.ENGINE.getBoard();

        // A move may only be submitted on the user's turn, and the bot may only be asked to move (no move) on its own
        // turn; otherwise a repeated or stale request would make the engine play the user's side.
        boolean botToMove = "BOT".equals(getPlayerType(board.isWhiteToMove() ? game.getWhitePlayer() : game.getBlackPlayer()));
        if (uChessRequest.getMove() == null && !botToMove) {
            throw new InvalidRequestException("It is not the bot's turn to move");
        }
        if (uChessRequest.getMove() != null && botToMove) {
            throw new InvalidRequestException("It is not your turn to move");
        }

        // If the side to move has run out of time, the game is over before this request can play anything
        long now = System.currentTimeMillis();
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

        if (gameResult == null) {
            // In a timed game the bot spends less than its usual second per move when its clock runs low
            boolean botIsWhite = board.isWhiteToMove();
            SearchResult result = UChessEngineUCI.ENGINE.thinkSync(GameClock.botThinkTime(game, botIsWhite, now));

            if (!GameClock.recordMove(game, botIsWhite, System.currentTimeMillis())) {
                // The bot ran out of time while thinking, so its move doesn't count
                finishOnTime(game, board, botIsWhite);
                return new UChessResponse(gameId, fen, null, null, game.getResult().name(),
                        GameClock.snapshot(game, botIsWhite, System.currentTimeMillis()), game.getTermination());
            }

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
            game.setEndTime(LocalDateTime.now());
        }
        gameRepository.save(game);

        return new UChessResponse(gameId, fen, bestMove, bestMoveSan, gameResult,
                GameClock.snapshot(game, board.isWhiteToMove(), System.currentTimeMillis()), game.getTermination());
    }

    /**
     * End the game because the given side ran out of time, and save it.
     */
    private void finishOnTime(Game game, Board board, boolean flaggedWhite) {
        GameClock.flag(game, board, flaggedWhite);
        game.setEndTime(LocalDateTime.now());
        gameRepository.save(game);
    }

    private GameDto toGameDto(Game game, Board board, UChessResponse botMove) {
        return new GameDto(
                game.getId(),
                game.getGameType(),
                new PlayerDto(game.getWhitePlayer().getId(), getPlayerType(game.getWhitePlayer()), getPlayerName(game.getWhitePlayer())),
                new PlayerDto(game.getBlackPlayer().getId(), getPlayerType(game.getBlackPlayer()), getPlayerName(game.getBlackPlayer())),
                game.getInitialFen(),
                game.getStartTime(),
                game.getEndTime(),
                game.getResult(),
                game.getMoveEntities().stream().map(m -> new MoveDto(m.getMoveNumber(), m.getUci())).toList(),
                botMove,
                board.isWhiteToMove() ? "WHITE" : "BLACK",
                GameClock.snapshot(game, board.isWhiteToMove(), System.currentTimeMillis()),
                game.getTermination()
        );
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

        Page<Game> games = gameRepository.findByWhitePlayerIdOrBlackPlayerIdOrderByStartTimeDesc(
                currentUser.getId(), currentUser.getId(), pageable);

        Page<GamesResponse> gamesResponses = games.map(game -> {
            String playerColor = game.getWhitePlayer().getId().equals(currentUser.getId()) ?
                    "WHITE" : "BLACK";
            Player opponent = playerColor.equals("WHITE") ?
                    game.getBlackPlayer() : game.getWhitePlayer();

            BotResponse botResponse = null;
            if ("BOT".equals(getPlayerType(opponent))) {
                Bots bot = botRepository.findById(opponent.getId())
                        .orElse(null);
                if (bot != null) {
                    botResponse = new BotResponse(bot);
                }
            }

            return new GamesResponse(
                    game.getId().toString(),
                    game.getGameType().name(),
                    getPlayerName(game.getWhitePlayer()),
                    getPlayerName(game.getBlackPlayer()),
                    game.getStartTime().toString(),
                    game.getEndTime() != null ? game.getEndTime().toString() : null,
                    game.getResult().name(),
                    game.getMoveEntities().size(),
                    playerColor,
                    botResponse
            );
        });

        // Calculate game statistics
        long totalGames = games.getTotalElements();
        long totalWins = games.stream().filter(game -> {
            boolean isWhite = game.getWhitePlayer().getId().equals(currentUser.getId());
            return (isWhite && game.getResult() == GameResult.WHITE_WIN) ||
                    (!isWhite && game.getResult() == GameResult.BLACK_WIN);
        }).count();
        long totalLosses = games.stream().filter(game -> {
            boolean isWhite = game.getWhitePlayer().getId().equals(currentUser.getId());
            return (isWhite && game.getResult() == GameResult.BLACK_WIN) ||
                    (!isWhite && game.getResult() == GameResult.WHITE_WIN);
        }).count();
        long totalDraws = games.stream().filter(game ->
                game.getResult() == GameResult.DRAW_BY_REPETITION ||
                        game.getResult() == GameResult.DRAW_BY_FIFTY_MOVE_RULE ||
                        game.getResult() == GameResult.DRAW_BY_INSUFFICIENT_MATERIAL ||
                        game.getResult() == GameResult.DRAW_BY_STALEMATE
        ).count();

        GameStatsResponse stats = new GameStatsResponse(
                (int) totalGames,
                (int) totalWins,
                (int) totalLosses,
                (int) totalDraws
        );

        return new HistoryResponse(stats, gamesResponses);
    }

    /**
     * Determines the type of player (USER or BOT) based on the JPA entity type.
     * Uses the TYPE function to retrieve the discriminator value, handling JPA proxies accurately.
     *
     * @param player the player entity to check
     * @return "USER" if the player is a UserEntity, "BOT" if the player is a Bots instance
     */
    private String getPlayerType(Player player) {
        log.debug("Checking player type for ID: {}", player.getId());
        try {
            Class<?> entityType = entityManager.createQuery(
                            "SELECT TYPE(p) FROM Player p WHERE p.id = :id", Class.class)
                    .setParameter("id", player.getId())
                    .getSingleResult();
            log.debug("Entity type for ID {}: {}", player.getId(), entityType.getSimpleName());
            return UserEntity.class.equals(entityType) ? "USER" : "BOT";
        } catch (Exception e) {
            log.warn("Failed to determine player type for ID: {}, defaulting to BOT", player.getId(), e);
            return "BOT";
        }
    }

    /**
     * Retrieves the display name of a player dynamically based on the entity type.
     * Returns the fullName for UserEntity or botName for Bots, falling back to a default name if not available.
     *
     * @param player the player entity to check
     * @return the player's display name
     */
    private String getPlayerName(Player player) {
        log.debug("Fetching name for player ID: {}", player.getId());
        try {
            Class<?> entityType = entityManager.createQuery(
                            "SELECT TYPE(p) FROM Player p WHERE p.id = :id", Class.class)
                    .setParameter("id", player.getId())
                    .getSingleResult();
            if (UserEntity.class.equals(entityType)) {
                String fullName = entityManager.createQuery(
                                "SELECT u.fullName FROM UserEntity u WHERE u.id = :id", String.class)
                        .setParameter("id", player.getId())
                        .getSingleResult();
                log.debug("User fullName for ID {}: {}", player.getId(), fullName);
                return fullName != null ? fullName : "User-" + player.getId();
            } else {
                String botName = entityManager.createQuery(
                                "SELECT b.botName FROM Bots b WHERE b.id = :id", String.class)
                        .setParameter("id", player.getId())
                        .getSingleResult();
                log.debug("Bot botName for ID {}: {}", player.getId(), botName);
                return botName != null ? botName : "Bot-" + player.getId();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch name for player ID: {}, defaulting to Player-{}", player.getId(), player.getId(), e);
            return "Player-" + player.getId();
        }
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