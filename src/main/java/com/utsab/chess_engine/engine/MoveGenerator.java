/**
 * Author: Utsab Dahal
 * User:LEGION
 * Date:3/10/2025
 * Time:2:11 PM
 */

package com.utsab.chess_engine.engine;

import com.utsab.chess_engine.model.Move;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    public static List<Move> generateMoves(Bitboard board) {
        List<Move> moves = new ArrayList<>();

        // Generate valid moves (Placeholder - Implement per piece)
        moves.add(new Move(12, 28, '-')); // e2 to e4
        moves.add(new Move(52, 36, '-')); // d7 to d5

        return moves;
    }
}
