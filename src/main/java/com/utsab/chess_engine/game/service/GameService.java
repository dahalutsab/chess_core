package com.utsab.chess_engine.game.service;

import com.utsab.chess_engine.bot.entity.Bots;
import com.utsab.chess_engine.bot.repository.BotRepository;
import com.utsab.chess_engine.core.chess.board.Board;
import com.utsab.chess_engine.core.chess.board.Move;
import com.utsab.chess_engine.core.chess.engine.Engine;
import com.utsab.chess_engine.core.chess.generation.MoveGeneration;
import com.utsab.chess_engine.core.chess.search.SearchResult;
import com.utsab.chess_engine.core.chess.utils.notation.FEN;
import com.utsab.chess_engine.core.chess.utils.notation.Notation;
import com.utsab.chess_engine.exceptions.custom.InvalidRequestException;
import com.utsab.chess_engine.game.dto.CreateGameRequest;
import com.utsab.chess_engine.game.dto.GameDto;
import com.utsab.chess_engine.game.dto.MakeMoveRequest;
import com.utsab.chess_engine.game.dto.MakeMoveResponse;
import com.utsab.chess_engine.game.dto.MoveDto;
import com.utsab.chess_engine.game.dto.PlayerDto;
import com.utsab.chess_engine.game.entity.Game;
import com.utsab.chess_engine.game.entity.enums.GameResult;
import com.utsab.chess_engine.game.entity.enums.GameType;
import com.utsab.chess_engine.game.repository.GameRepository;
import com.utsab.chess_engine.move.entity.MoveEntity;
import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Player vs. Bot chess games, integrating with a UCI-compatible chess engine.
 * Handles game creation, move validation, and bot responses, storing moves in UCI notation (e.g., "e2e4").
 * Ensures compatibility with existing game entities, DTOs, and engine operations.
 *
 * @author Utsab Dahal
 * @since 2025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private static final int DEFAULT_THINK_TIME_MS = 1000;
    private static final int ENGINE_TIMEOUT_MS = 2000;
    private static final String WHITE = "WHITE";
    private static final String BLACK = "BLACK";

    private final GameRepository gameRepository;
    private final UserInfoRepository userRepository;
    private final BotRepository botRepository;
    private final Engine engine;
    private final MoveGeneration moveGenerator;

    /**
     * Creates a new Player vs. Bot chess game based on the provided request. Initializes the game with
     * the specified FEN, assigns players, and persists the game state. If the bot is Black and it's
     * Black's turn (determined by FEN), generates and stores the bot's first move.
     * <p>
     * Maps to UCI commands: <code>ucinewgame</code>, <code>position</code>, and <code>go</code> (if bot moves first).
     *
     * @param request the request containing game type, player ID, bot ID, player color, and optional initial FEN
     * @return a DTO representing the created game, including bot move if applicable
     * @throws InvalidRequestException if the player, bot, or color is invalid
     * @author Utsab Dahal
     */
    @Transactional
    public GameDto createGame(CreateGameRequest request) {
        log.info("Creating game for playerId: {}, botId: {}, color: {}", request.playerId(), request.botId(), request.playerColor());
        // Validate request
        UserEntity user = userRepository.findById(request.playerId())
                .orElseThrow(() -> new InvalidRequestException("User not found: " + request.playerId()));
        Bots bot = botRepository.findById(request.botId())
                .orElseThrow(() -> new InvalidRequestException("Bot not found: " + request.botId()));
        if (!List.of(WHITE, BLACK).contains(request.playerColor())) {
            throw new InvalidRequestException("Invalid player color: " + request.playerColor());
        }

        // Initialize engine for new game
        engine.gameOver();
        engine.newGame();

        // Set up game entity
        String initialFen = Objects.requireNonNullElse(request.initialFen(), FEN.STARTING_POSITION);
        Game game = Game.builder()
                .whitePlayer(request.playerColor().equals(WHITE) ? user : bot)
                .blackPlayer(request.playerColor().equals(BLACK) ? user : bot)
                .initialFen(initialFen)
                .startTime(LocalDateTime.now())
                .result(GameResult.IN_PROGRESS)
                .moveEntities(new ArrayList<>())
                .build();

        // Persist game
        game = gameRepository.save(game);

        MoveDto botMove = null;
        // Check if bot (Black) needs to move first
        boolean isWhiteToMove = FEN.toBoard(initialFen).isWhiteToMove();
        if (request.playerColor().equals(WHITE) && !isWhiteToMove) {
            log.info("Bot (Black) to move first for gameId: {}", game.getId());
            botMove = generateBotMove(game, initialFen, new ArrayList<>());
        }

        // Build response DTO
        return new GameDto(
                game.getId(),
                request.gameType(),
                new PlayerDto(game.getWhitePlayer().getId(), user.equals(game.getWhitePlayer()) ? "USER" : "BOT",
                        "Player-" + game.getWhitePlayer().getId()),
                new PlayerDto(game.getBlackPlayer().getId(), user.equals(game.getBlackPlayer()) ? "USER" : "BOT",
                        "Player-" + game.getBlackPlayer().getId()),
                initialFen,
                game.getStartTime(),
                game.getEndTime(),
                game.getResult(),
                game.getMoveEntities().stream()
                        .map(m -> new MoveDto(m.getMoveNumber(), m.getUci()))
                        .toList(),
                botMove
        );
    }

    /**
     * Processes a player's move in an existing game, validates the move, and generates a bot response if
     * the game is ongoing. Updates the game state, persists moves, and checks for termination conditions.
     * Reconstructs the board state using the game's initial FEN and prior moves from MoveEntity.
     * <p>
     * Maps to UCI commands: <code>position</code>, <code>go</code>.
     *
     * @param gameId  the UUID of the game
     * @param request the request containing the move in UCI notation (e.g., "e2e4")
     * @return a response containing the updated game state, including the player's move and bot's response
     * @throws InvalidRequestException if the game, move, or game state is invalid
     * @author Utsab Dahal
     */
    @Transactional
    public MakeMoveResponse makeMove(UUID gameId, MakeMoveRequest request) {
        log.info("Processing move for gameId: {}, move: {}", gameId, request.san());
        // Load game
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new InvalidRequestException("Game not found: " + gameId));

        // Reconstruct board state
        String initialFen = game.getInitialFen();
        List<Move> priorMoves = game.getMoveEntities().stream()
                .map(m -> Notation.fromCombinedNotation(m.getUci()))
                .toList();
        log.debug("Setting position with FEN: {}, moves: {}", initialFen, priorMoves);
        engine.setPosition(initialFen, priorMoves);
        Board board = engine.getBoard();

        // Validate and parse player's move (expecting UCI, e.g., "e2e4")
        Move playerMove = Notation.fromCombinedNotation(request.san());
        List<Move> legalMoves = moveGenerator.generateMoves(board);
        if (!legalMoves.contains(playerMove)) {
            throw new InvalidRequestException("Illegal move: " + request.san());
        }

        // Apply player's move
        log.debug("Applying player move: {}", playerMove);
        board.makeMove(playerMove);
        String newFen = FEN.toFen(board);
        float moveNumber = calculateMoveNumber(game.getMoveEntities().size() + 1);
        String uci = Notation.toNotation(playerMove);
        MoveEntity playerMoveEntity = MoveEntity.builder()
                .game(game)
                .moveNumber(moveNumber)
                .uci(uci)
                .build();
        game.getMoveEntities().add(playerMoveEntity);

        // Check game result
        GameResult result = determineGameResult(board);
        MoveDto botMove = null;
        if (result == GameResult.IN_PROGRESS) {
            // Generate bot's response
            log.info("Generating bot move for gameId: {}", gameId);
            botMove = generateBotMove(game, initialFen, game.getMoveEntities().stream()
                    .map(m -> Notation.fromCombinedNotation(m.getUci()))
                    .toList());
            result = determineGameResult(engine.getBoard());
        }

        // Update game state
        if (result != GameResult.IN_PROGRESS) {
            game.setEndTime(LocalDateTime.now());
            game.setResult(result);
        }
        gameRepository.save(game);

        // Build response
        return new MakeMoveResponse(
                gameId,
                new MoveDto(moveNumber, uci),
                newFen,
                result,
                botMove
        );
    }

    /**
     * Generates a bot move for the current game position, persists it, and updates the game state.
     * Uses the provided FEN and prior moves to set up the board, then invokes the engine to compute the move.
     *
     * @param game       the game entity
     * @param fen        the initial FEN position
     * @param priorMoves the list of prior moves in the game
     * @return a DTO representing the bot's move
     * @throws InvalidRequestException if the engine fails to generate a move
     * @author Utsab Dahal
     */
    private MoveDto generateBotMove(Game game, String fen, List<Move> priorMoves) {
        // Set up position
        log.debug("Setting bot position with FEN: {}, moves: {}", fen, priorMoves);
        engine.setPosition(fen, priorMoves);
        Board board = engine.getBoard();

        // Validate board state
        if (moveGenerator.generateMoves(board).isEmpty() && !moveGenerator.isCheck(board, board.isWhiteToMove())) {
            throw new InvalidRequestException("Invalid board state: no legal moves and not in check");
        }

        // Think and capture the best move
        CompletableFuture<Move> moveFuture = new CompletableFuture<>();
        engine.think(DEFAULT_THINK_TIME_MS, result -> moveFuture.complete(result.move()));
        Move bestMove;
        try {
            bestMove = moveFuture.get(ENGINE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Engine failed to generate move for position: {}, moves: {}", fen, priorMoves, e);
            throw new InvalidRequestException("Engine failed to generate a move", e);
        }

        if (bestMove == null) {
            log.error("Engine returned null move for position: {}, moves: {}", fen, priorMoves);
            throw new InvalidRequestException("Engine returned null move");
        }

        // Apply bot move
        log.debug("Applying bot move: {}", bestMove);
        board.makeMove(bestMove);
        String uci = Notation.toNotation(bestMove);
        float moveNumber = calculateMoveNumber(game.getMoveEntities().size() + 1);
        MoveEntity botMoveEntity = MoveEntity.builder()
                .game(game)
                .moveNumber(moveNumber)
                .uci(uci)
                .build();
        game.getMoveEntities().add(botMoveEntity);

        return new MoveDto(moveNumber, uci, engine.extractPrincipalVariation().stream()
                .map(Notation::toNotation)
                .toList());
    }

    /**
     * Calculates the move number based on the total number of moves played.
     * White's moves are whole numbers (e.g., 1.0, 2.0), Black's are half numbers (e.g., 1.5, 2.5).
     *
     * @param totalMoves the total number of moves played
     * @return the move number as a float
     * @author Utsab Dahal
     */
    private float calculateMoveNumber(int totalMoves) {
        return totalMoves % 2 == 1 ? (totalMoves + 1) / 2.0f : totalMoves / 2.0f + 0.5f;
    }

    /**
     * Determines the game result based on the current board state by checking for checkmate, stalemate,
     * or the fifty-move rule.
     *
     * @param board the current chess board
     * @return the game result (e.g., WHITE_WIN, BLACK_WIN, DRAW, IN_PROGRESS)
     * @author Utsab Dahal
     */
    private GameResult determineGameResult(Board board) {
        List<Move> legalMoves = moveGenerator.generateMoves(board);
        if (legalMoves.isEmpty()) {
            if (moveGenerator.isCheck(board, board.isWhiteToMove())) {
                return board.isWhiteToMove() ? GameResult.BLACK_WIN : GameResult.WHITE_WIN;
            }
            return GameResult.DRAW; // Stalemate
        }
        if (board.getGameState().getFiftyMoveCounter() >= 100) {
            return GameResult.DRAW; // 50-move rule
        }
        return GameResult.IN_PROGRESS;
    }
}