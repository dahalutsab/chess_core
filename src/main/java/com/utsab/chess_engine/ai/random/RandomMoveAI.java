package com.utsab.chess_engine.ai.random;

import com.utsab.chess_engine.board.Move;
import com.utsab.chess_engine.board.MoveGenerator;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class RandomMoveAI {
    private static final Logger logger = Logger.getLogger(RandomMoveAI.class.getName());
    private final MoveGenerator moveGenerator;
    private final Random random;

    public RandomMoveAI(MoveGenerator moveGenerator) {
        this.moveGenerator = moveGenerator;
        this.random = new Random();
    }

    public Move getRandomMove() {
        List<Move> moves = moveGenerator.generateMoves();
        if (moves.isEmpty()) {
            logger.info("No valid moves available.");
            return null;
        }
        Move randomMove = moves.get(random.nextInt(moves.size()));
        logger.info("Selected random move: " + randomMove.toString());
        return randomMove;
    }
}