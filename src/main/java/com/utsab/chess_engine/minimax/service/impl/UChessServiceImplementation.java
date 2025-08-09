package com.utsab.chess_engine.minimax.service.impl;

import com.utsab.chess_engine.bot.entity.Bots;
import com.utsab.chess_engine.bot.repository.BotRepository;
import com.utsab.chess_engine.common.entity.Player;
import com.utsab.chess_engine.core.UChessEngineUCI;
import com.utsab.chess_engine.core.board.Move;
import com.utsab.chess_engine.core.evaluation.Result;
import com.utsab.chess_engine.core.search.SearchResult;
import com.utsab.chess_engine.core.utils.notation.FEN;
import com.utsab.chess_engine.core.utils.notation.Notation;
import com.utsab.chess_engine.core.utils.notation.SAN;
import com.utsab.chess_engine.exceptions.custom.InvalidRequestException;
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
import com.utsab.chess_engine.minimax.dto.NewGameRequest;
import com.utsab.chess_engine.minimax.dto.PreferredColor;
import com.utsab.chess_engine.minimax.dto.UChessRequest;
import com.utsab.chess_engine.minimax.dto.UChessResponse;
import com.utsab.chess_engine.minimax.service.UChessService;
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
        UChessEngineUCI.ENGINE.gameOver();
        UChessEngineUCI.ENGINE.newGame();

        UserEntity user = loggedInUserUtil.getLoggedInUser();
        Bots bot = botRepository.findBySlug(request.botSlug())
                .orElseThrow(() -> new InvalidRequestException("Bot not found: " + request.botSlug()));

        String initialFen = Objects.requireNonNullElse(request.initialFen(), FEN.STARTING_POSITION);

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

        game = gameRepository.save(game);

        UChessResponse botMove = null;
        if (color == PreferredColor.BLACK) {
            UChessRequest uChessRequest = new UChessRequest(game.getId(), null);
            botMove = getBestMove(uChessRequest);
        }

        UChessEngineUCI.ENGINE.setPosition(initialFen, new ArrayList<>());
        String currentTurn = UChessEngineUCI.ENGINE.getBoard().isWhiteToMove() ? "WHITE" : "BLACK";

        return new GameDto(
                game.getId(),
                request.gameType(),
                new PlayerDto(game.getWhitePlayer().getId(), whitePlayer instanceof UserEntity ? "USER" : "BOT", getPlayerName(whitePlayer)),
                new PlayerDto(game.getBlackPlayer().getId(), blackPlayer instanceof UserEntity ? "USER" : "BOT", getPlayerName(blackPlayer)),
                initialFen,
                game.getStartTime(),
                game.getEndTime(),
                game.getResult(),
                game.getMoveEntities().stream().map(m -> new MoveDto(m.getMoveNumber(), m.getUci())).toList(),
                botMove,
                currentTurn
        );
    }

    /**
     * Computes the best move for the current game state using the chess engine.
     * Validates the provided move (if any) and updates the game with the bot's response move.
     *
     * @param uChessRequest the request containing the game ID and optional user move in UCI notation
     * @return a response containing the game ID, current FEN, best move, and SAN notation
     * @throws InvalidRequestException if the game ID or move is invalid
     */
    @Override
    public UChessResponse getBestMove(UChessRequest uChessRequest) {
        Game game = gameRepository.findById(uChessRequest.getGameId())
                .orElseThrow(() -> new InvalidRequestException("Game not found: " + uChessRequest.getGameId()));

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
        List<Move> moveList = new ArrayList<>();
        if (!moves.isEmpty()) {
            List<String> uciMoves = moves.stream().map(MoveEntity::getUci).toList();
            moveList = uciMoves.stream().map(Notation::fromCombinedNotation).toList();
        }

        String fen = game.getInitialFen();
        UUID gameId = uChessRequest.getGameId();

        UChessEngineUCI.ENGINE.setPosition(fen, moveList);
        List<Move> validMoves = UChessEngineUCI.ENGINE.getMoveGenerator().generateMoves(UChessEngineUCI.ENGINE.getBoard());

        if (uChessRequest.getMove() != null &&
                validMoves.stream().noneMatch(m -> Notation.toNotation(m).equals(uChessRequest.getMove()))) {
            throw new InvalidRequestException("Invalid move requested: " + uChessRequest.getMove());
        }

        if (uChessRequest.getMove() != null) {
            MoveEntity moveEntity = MoveEntity.builder()
                    .game(game)
                    .moveNumber(calculateMoveNumber(moves.size() + 1))
                    .uci(uChessRequest.getMove())
                    .build();
            game.getMoveEntities().add(moveEntity);
            gameRepository.save(game);
            moveList = new ArrayList<>(moveList);
            moveList.add(Notation.fromCombinedNotation(uChessRequest.getMove()));
            UChessEngineUCI.ENGINE.setPosition(fen, moveList);
        }

        SearchResult result = UChessEngineUCI.ENGINE.thinkSync(1000);
        String bestMove = Notation.toNotation(result.move());
        String bestMoveSan = SAN.toSan(result.move(), UChessEngineUCI.ENGINE.getBoard());

        // First check if there's a result before the bot's move
        String gameResult = null;
        if (Result.isDoubleRepetition(UChessEngineUCI.ENGINE.getBoard())) {
            gameResult = "DRAW_BY_REPETITION";
        } else if (Result.isFiftyMoveRule(UChessEngineUCI.ENGINE.getBoard())) {
            gameResult = "DRAW_BY_FIFTY_MOVE_RULE";
        } else if (Result.isInsufficientMaterial(UChessEngineUCI.ENGINE.getBoard())) {
            gameResult = "DRAW_BY_INSUFFICIENT_MATERIAL";
        } else if (UChessEngineUCI.ENGINE.getMoveGenerator().generateMoves(UChessEngineUCI.ENGINE.getBoard()).isEmpty()) {
            if (UChessEngineUCI.ENGINE.getMoveGenerator().isCheck(UChessEngineUCI.ENGINE.getBoard(),
                    UChessEngineUCI.ENGINE.getBoard().isWhiteToMove())) {
                gameResult = UChessEngineUCI.ENGINE.getBoard().isWhiteToMove() ? "BLACK_WIN" : "WHITE_WIN";
            } else {
                gameResult = "STALEMATE";
            }
        }

        // If no result yet, apply the bot's move and check again
        if (gameResult == null) {
            Move move = Notation.fromCombinedNotation(bestMove);
            UChessEngineUCI.ENGINE.getBoard().makeMove(move);

            if (Result.isDoubleRepetition(UChessEngineUCI.ENGINE.getBoard())) {
                gameResult = "DRAW_BY_REPETITION";
            } else if (Result.isFiftyMoveRule(UChessEngineUCI.ENGINE.getBoard())) {
                gameResult = "DRAW_BY_FIFTY_MOVE_RULE";
            } else if (Result.isInsufficientMaterial(UChessEngineUCI.ENGINE.getBoard())) {
                gameResult = "DRAW_BY_INSUFFICIENT_MATERIAL";
            } else if (UChessEngineUCI.ENGINE.getMoveGenerator().generateMoves(UChessEngineUCI.ENGINE.getBoard()).isEmpty()) {
                if (UChessEngineUCI.ENGINE.getMoveGenerator().isCheck(UChessEngineUCI.ENGINE.getBoard(),
                        UChessEngineUCI.ENGINE.getBoard().isWhiteToMove())) {
                    gameResult = UChessEngineUCI.ENGINE.getBoard().isWhiteToMove() ? "BLACK_WIN" : "WHITE_WIN";
                } else {
                    gameResult = "STALEMATE";
                }
            }
        }

        // Update game result if needed
        if (gameResult != null) {
            game.setResult(GameResult.valueOf(gameResult));
            game.setEndTime(LocalDateTime.now());
        }

        float moveNumber = calculateMoveNumber(game.getMoveEntities().size() + 1);
        MoveEntity moveEntity = MoveEntity.builder()
                .game(game)
                .moveNumber(moveNumber)
                .uci(bestMove)
                .build();
        game.getMoveEntities().add(moveEntity);
        gameRepository.save(game);

        return new UChessResponse(gameId, fen, bestMove, bestMoveSan, gameResult);
    }

    /**
     * Retrieves detailed information about a specific chess game.
     * Includes player details, move history, and current game state.
     *
     * @param gameId the unique identifier of the game
     * @return a DTO containing the game’s metadata and state
     * @throws InvalidRequestException if the game ID is invalid
     */
    @Override
    public GameDto getGameInfo(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new InvalidRequestException("Game not found: " + gameId));

        String fen = game.getInitialFen();
        List<MoveEntity> moves = game.getMoveEntities();
        List<String> uciMoves = moves.stream().map(MoveEntity::getUci).toList();

        UChessEngineUCI.ENGINE.setPosition(fen, uciMoves.stream().map(Notation::fromCombinedNotation).toList());
        String currentTurn = UChessEngineUCI.ENGINE.getBoard().isWhiteToMove() ? "WHITE" : "BLACK";

        return new GameDto(
                game.getId(),
                game.getGameType(),
                new PlayerDto(game.getWhitePlayer().getId(), getPlayerType(game.getWhitePlayer()), getPlayerName(game.getWhitePlayer())),
                new PlayerDto(game.getBlackPlayer().getId(), getPlayerType(game.getBlackPlayer()), getPlayerName(game.getBlackPlayer())),
                fen,
                game.getStartTime(),
                game.getEndTime(),
                game.getResult(),
                moves.stream().map(m -> new MoveDto(m.getMoveNumber(), m.getUci())).toList(),
                null,
                currentTurn
        );
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