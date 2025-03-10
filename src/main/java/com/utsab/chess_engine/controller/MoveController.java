/**
 * Author: Utsab Dahal
 * User:LEGION
 * Date:3/10/2025
 * Time:2:12 PM
 */

package com.utsab.chess_engine.controller;

import com.utsab.chess_engine.engine.Bitboard;
import com.utsab.chess_engine.engine.RandomMoveGenerator;
import com.utsab.chess_engine.model.Move;
import com.utsab.chess_engine.util.FenParser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chess")
public class MoveController {

    @GetMapping("/random-move")
    public String getRandomMove(@RequestParam String fen) {
        Bitboard board = FenParser.fromFEN(fen);
        Move move = RandomMoveGenerator.getRandomMove(board);

        return move != null ? move.toString() : "No legal moves available";
    }
}
