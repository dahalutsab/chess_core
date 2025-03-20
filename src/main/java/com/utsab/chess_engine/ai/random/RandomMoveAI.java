package com.utsab.chess_engine.ai.random;

import com.utsab.chess_engine.board.Move;
import com.utsab.chess_engine.board.MoveGenerator;

import java.util.List;
import java.util.Random;

public class RandomMoveAI {
    private final MoveGenerator moveGenerator;

    public RandomMoveAI(MoveGenerator moveGenerator) {
        this.moveGenerator = moveGenerator;
    }

    public Move getRandomMove() {
        List<Move> moves = moveGenerator.generateMoves();
        if (moves.isEmpty()) {
            return null; // No valid moves (checkmate or stalemate)
        }
        Random random = new Random();
        return moves.get(random.nextInt(moves.size()));
    }
}