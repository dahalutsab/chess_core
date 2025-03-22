package com.utsab.chess_engine.ai.random;

import com.utsab.chess_engine.board.Bitboard;
import com.utsab.chess_engine.board.Move;
import com.utsab.chess_engine.board.MoveGenerator;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * A simple AI that selects a random legal move from the current position.
 */
public class RandomMoveAI {
    private static final Logger logger = Logger.getLogger(RandomMoveAI.class.getName());
    private final MoveGenerator moveGenerator;
    private final Random random;

    public RandomMoveAI(MoveGenerator moveGenerator) {
        this.moveGenerator = moveGenerator;
        this.random = new Random();
    }

    /**
     * Selects a random legal move from the current position.
     *
     * @param board the current chess position
     * @return a random Move, or null if no legal moves exist
     */
    public Move findRandomMove(Bitboard board) {
        List<Move> moves = moveGenerator.generateMoves();
        if (moves.isEmpty()) {
            logger.info("No legal moves available.");
            return null;
        }
        Move move = moves.get(random.nextInt(moves.size()));
        logger.info("Selected random move: " + move);
        return move;
    }
}