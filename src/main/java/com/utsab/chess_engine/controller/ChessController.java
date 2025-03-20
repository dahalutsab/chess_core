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
        MoveGenerator moveGenerator = new MoveGenerator(null);
        Bitboard bitboard = FenParser.loadFromFen(fen, moveGenerator);
        moveGenerator.setBitboard(bitboard);
        RandomMoveAI ai = new RandomMoveAI(moveGenerator);
        Move move = ai.getRandomMove();
        return move != null ? move.toString() : "No valid moves";
    }
}