package com.utsab.chess_engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utsab.chess_engine.ai.minimax.MinimaxAI;
import com.utsab.chess_engine.ai.random.RandomMoveAI;
import com.utsab.chess_engine.board.Bitboard;
import com.utsab.chess_engine.board.FenParser;
import com.utsab.chess_engine.board.Move;
import com.utsab.chess_engine.board.MoveGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST controller for chess engine operations, supporting random and Minimax-based move generation.
 */
@RestController
@RequestMapping("/api")
public class ChessController {
    private static final Logger logger = Logger.getLogger(ChessController.class.getName());
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Response DTO for move endpoints.
     */
    private record MoveResponse(String bestMove, String evaluation, int depth, long nodes, long nps,
                                List<String> pv, long timeTakenMs) {}

    /**
     * Generates a random legal move for the given FEN position.
     *
     * @param fen the FEN string representing the position
     * @return a JSON response with the random move and metadata
     * @throws ResponseStatusException if the FEN is invalid
     */
    @PostMapping("/random-move")
    public String getRandomMove(@RequestParam String fen) {
        try {
            Bitboard board = FenParser.loadFromFen(fen);
            MoveGenerator moveGenerator = new MoveGenerator(board);
            RandomMoveAI ai = new RandomMoveAI(moveGenerator);

            long startTime = System.nanoTime();
            Move move = ai.findRandomMove(board);
            long timeTakenMs = (System.nanoTime() - startTime) / 1_000_000;

            MoveResponse response = new MoveResponse(
                    move != null ? move.toString() : null,
                    move != null ? "0.00" : (board.isInCheck(board.isWhiteToMove()) ? "-M1" : "0.00"),
                    0, // Depth not applicable for random
                    0, // Nodes not tracked for random
                    0, // NPS not applicable
                    move != null ? List.of(move.toString()) : List.of(),
                    timeTakenMs
            );

            String jsonResponse = objectMapper.writeValueAsString(response);
            logger.info("Random move response: " + jsonResponse);
            return jsonResponse;
        } catch (Exception e) {
            logger.severe("Error processing random move for FEN " + fen + ": " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid FEN: " + e.getMessage());
        }
    }

    /**
     * Generates the best move using Minimax for the given FEN position and depth.
     *
     * @param fen   the FEN string representing the position
     * @param depth the search depth for Minimax
     * @return a JSON response with the best move, evaluation, and metadata
     * @throws ResponseStatusException if the FEN is invalid or depth is out of range
     */
    @CrossOrigin(allowCredentials = "true", origins = "http://localhost:4200")
    @GetMapping("/minimax-move")
    public String getMinimaxMove(@RequestParam String fen, @RequestParam int depth) {
        if (depth < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Depth must be at least 1");
        }

        try {
            Bitboard board = FenParser.loadFromFen(fen);
            MoveGenerator moveGenerator = new MoveGenerator(board);
            MinimaxAI ai = new MinimaxAI(moveGenerator);

            long startTime = System.nanoTime();
            MinimaxAI.MinimaxResult result = ai.findBestMove(board, depth);
            long timeTakenMs = (System.nanoTime() - startTime) / 1_000_000;

            MoveResponse response;
            if (result.bestMove() == null) {
                String eval = board.isInCheck(board.isWhiteToMove()) ? "-M1" : "0.00";
                response = new MoveResponse(null, eval, depth, result.nodes(), 0, List.of(), timeTakenMs);
            } else {
                long nps = timeTakenMs > 0 ? (result.nodes() * 1000 / timeTakenMs) : 0;
                response = new MoveResponse(
                        result.bestMove().toString(),
                        String.format("%+.2f", result.bestScore() / 100.0),
                        depth,
                        result.nodes(),
                        nps,
                        result.principalVariation().stream().map(Move::toString).collect(Collectors.toList()),
                        timeTakenMs
                );
            }

            String jsonResponse = objectMapper.writeValueAsString(response);
            logger.info("Minimax response: " + jsonResponse);
            return jsonResponse;
        } catch (Exception e) {
            logger.severe("Error processing Minimax move for FEN " + fen + ": " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid FEN: " + e.getMessage());
        }
    }
}