package com.utsab.chess_engine.minimax.service.impl;

import com.utsab.chess_engine.bot.entity.Bots;
import com.utsab.chess_engine.bot.repository.BotRepository;
import com.utsab.chess_engine.common.entity.Player;
import com.utsab.chess_engine.core.chess.Core;
import com.utsab.chess_engine.core.chess.board.Move;
import com.utsab.chess_engine.core.chess.search.SearchResult;
import com.utsab.chess_engine.core.chess.utils.notation.FEN;
import com.utsab.chess_engine.core.chess.utils.notation.Notation;
import com.utsab.chess_engine.core.chess.utils.notation.SAN;
import com.utsab.chess_engine.exceptions.custom.InvalidRequestException;
import com.utsab.chess_engine.game.dto.GameDto;
import com.utsab.chess_engine.game.dto.MoveDto;
import com.utsab.chess_engine.game.dto.PlayerDto;
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
import com.utsab.chess_engine.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of the UChessService interface.
 * This service is responsible for handling chess-related operations.
 * @author Utsab Dahal
 * @version 1.0
 * @since 2023-10-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UChessServiceImplementation implements UChessService {

    private final GameRepository gameRepository;
    private final UserInfoRepository userRepository;
    private final BotRepository botRepository;

    @Override
    public GameDto startNewGame(NewGameRequest request) {
        Core.ENGINE.gameOver();
        Core.ENGINE.newGame();

        // Validate request
        UserEntity user = userRepository.findById(request.playerId())
                .orElseThrow(() -> new InvalidRequestException("User not found: " + request.playerId()));
        Bots bot = botRepository.findById(request.botId())
                .orElseThrow(() -> new InvalidRequestException("Bot not found: " + request.botId()));

        // Set up game entity
        String initialFen = Objects.requireNonNullElse(request.initialFen(), FEN.STARTING_POSITION);

        Player whitePlayer = request.playerColor() == PreferredColor.WHITE ? user : bot;
        Player blackPlayer = request.playerColor() == PreferredColor.BLACK ? user : bot;

        Game game = Game.builder()
                .whitePlayer(whitePlayer)
                .blackPlayer(blackPlayer)
                .initialFen(initialFen)
                .startTime(LocalDateTime.now())
                .result(GameResult.IN_PROGRESS)
                .moveEntities(new ArrayList<>())
                .build();

        // Persist game
        game = gameRepository.save(game);

        UChessResponse botMove = null;
        // Check if bot (WHITE) needs to move first
        if (request.playerColor() == PreferredColor.BLACK) {
            UChessRequest uChessRequest = new UChessRequest(game.getId(), null);
            botMove = getBestMove(uChessRequest);
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

    @Override
    public UChessResponse getBestMove(UChessRequest uChessRequest) {
        Game game = gameRepository.findById(uChessRequest.getGameId())
                .orElseThrow(() -> new InvalidRequestException("Game not found: " + uChessRequest.getGameId()));

        // Get the max depth from the bot if available, otherwise use a default value
        int maxDepth = game.getBlackPlayer() instanceof Bots bot ? bot.getMaxDepth() : game.getWhitePlayer() instanceof Bots bot ? bot.getMaxDepth() : 200;

        // Set the engine's max depth if the method exists
        Core.ENGINE.getConfig().setMaxDepth(maxDepth);

        //log max depth
        log.info("Max depth set to: {}", Core.ENGINE.getConfig().getMaxDepth());

        List<MoveEntity> moves = new ArrayList<>(game.getMoveEntities());
        List<Move> moveList = new ArrayList<>();
        if (!moves.isEmpty()) {
            List<String> uciMoves = moves.stream()
                    .map(MoveEntity::getUci)
                    .toList();
            moveList = uciMoves.stream()
                    .map(Notation::fromCombinedNotation)
                    .toList();
        }

        String fen = game.getInitialFen();
        UUID gameId = uChessRequest.getGameId();

        // Set the position in the engine before validating the move
        Core.ENGINE.setPosition(fen, moveList);

        // Generate valid moves from the correct position
        List<Move> validMoves = Core.ENGINE.getMoveGenerator().generateMoves(Core.ENGINE.getBoard());

        // Check if requested move is in valid moves
        if (uChessRequest.getMove() != null &&
                validMoves.stream().noneMatch(m -> Notation.toNotation(m).equals(uChessRequest.getMove()))) {
            throw new InvalidRequestException("Invalid move requested: " + uChessRequest.getMove());
        }

        // If a move is provided, save it to the game and update moveList
        if (uChessRequest.getMove() != null) {
            MoveEntity moveEntity = MoveEntity.builder()
                    .game(game)
                    .moveNumber(calculateMoveNumber(moves.size() + 1))
                    .uci(uChessRequest.getMove())
                    .build();
            game.getMoveEntities().add(moveEntity);
            gameRepository.save(game);
            moveList = new ArrayList<>(moveList); // ensure modifiable
            moveList.add(Notation.fromCombinedNotation(uChessRequest.getMove()));
            // Update engine position after player's move
            Core.ENGINE.setPosition(fen, moveList);
        }

        // Think for a default time (e.g., 1000ms)
        SearchResult result = Core.ENGINE.thinkSync(1000);

        String bestMove = Notation.toNotation(result.move());
        String bestMoveSan = SAN.toSan(result.move(), Core.ENGINE.getBoard());

        float moveNumber = calculateMoveNumber(game.getMoveEntities().size() + 1);
        // Create a new MoveEntity for the best move
        MoveEntity moveEntity = MoveEntity.builder()
                .game(game)
                .moveNumber(moveNumber)
                .uci(bestMove)
                .build();
        game.getMoveEntities().add(moveEntity);
        gameRepository.save(game);

        return new UChessResponse(gameId, fen, bestMove, bestMoveSan);
    }

    /**
     * Calculates the move number based on the total number of moves played. White's moves are whole
     * numbers (e.g., 1.0, 2.0), and Black's moves are half numbers (e.g., 1.5, 2.5). Ensures accurate
     * move numbering for game state persistence and UCI notation.
     *
     * @param totalMoves the total number of moves played in the game
     * @return the move number as a float
     * @author Utsab Dahal
     */
    private float calculateMoveNumber(int totalMoves) {
        return totalMoves % 2 == 1 ? (totalMoves + 1) / 2.0f : totalMoves / 2.0f + 0.5f;
    }
}
