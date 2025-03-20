package com.utsab.chess_engine.controller;

import com.utsab.chess_engine.ai.minimax.MinimaxAI;
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
        Bitboard bitboard = FenParser.loadFromFen(fen);
        moveGenerator.setBitboard(bitboard);
        RandomMoveAI ai = new RandomMoveAI(moveGenerator);
        Move move = ai.findMove();
        return move != null ? move.toString() : "No valid moves";
    }

    @PostMapping("/minimax-move")
    public String getMinimaxMove(@RequestParam String fen, @RequestParam int depth) {
        MoveGenerator moveGenerator = new MoveGenerator(null);
        Bitboard bitboard = FenParser.loadFromFen(fen);
        moveGenerator.setBitboard(bitboard);
        MinimaxAI ai = new MinimaxAI(moveGenerator);
        Move move = ai.findBestMove(bitboard, depth);
        return move != null ? move.toString() : "No valid moves";
    }
}