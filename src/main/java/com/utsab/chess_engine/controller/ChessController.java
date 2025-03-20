package com.utsab.chess_engine.controller;

import com.utsab.chess_engine.ai.random.RandomMoveAI;
import com.utsab.chess_engine.board.Bitboard;
import com.utsab.chess_engine.board.FenParser;
import com.utsab.chess_engine.board.Move;
import com.utsab.chess_engine.board.MoveGenerator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chess")
public class ChessController {

    @PostMapping("/random-move")
    public String getRandomMove(@RequestParam String fen) {
        Bitboard bitboard = new Bitboard();
        FenParser.loadFromFen(bitboard, fen);

        // Log the current position
        bitboard.logPosition();

        MoveGenerator moveGenerator = new MoveGenerator(bitboard);
        moveGenerator.logValidMoves();

        RandomMoveAI randomMoveAI = new RandomMoveAI(moveGenerator);
        Move randomMove = randomMoveAI.getRandomMove();

        return randomMove != null ? randomMove.toString() : "No valid moves";
    }
}