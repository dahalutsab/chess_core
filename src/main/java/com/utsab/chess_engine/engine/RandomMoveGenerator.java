/**
 * Author: Utsab Dahal
 * User:LEGION
 * Date:3/10/2025
 * Time:2:12 PM
 */

package com.utsab.chess_engine.engine;

import com.utsab.chess_engine.model.Move;

import java.util.List;
import java.util.Random;

public class RandomMoveGenerator {
    public static Move getRandomMove(Bitboard board) {
        List<Move> legalMoves = MoveGenerator.generateMoves(board);
        if (legalMoves.isEmpty()) return null; // No moves (checkmate or stalemate)

        Random random = new Random();
        return legalMoves.get(random.nextInt(legalMoves.size()));
    }
}