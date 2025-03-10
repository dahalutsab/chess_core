/**
 * Author: Utsab Dahal
 * User:LEGION
 * Date:3/10/2025
 * Time:2:07 PM
 */

package com.utsab.chess_engine.controller;
import com.utsab.chess_engine.engine.Bitboard;
import com.utsab.chess_engine.util.FenParser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chess")
public class ChessController {

    @GetMapping("/fen")
    public String getBitboard(@RequestParam String fen) {
        Bitboard board = FenParser.fromFEN(fen);
        return board.getBoardState() + "";
    }
}
