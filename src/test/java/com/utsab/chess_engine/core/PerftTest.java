package com.utsab.chess_engine.core;

import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.Move;
import com.utsab.chess_engine.core.generation.MoveGenerator;
import com.utsab.chess_engine.core.utils.notation.FEN;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Counts leaf nodes of the legal move tree for well-known positions and compares them with published values. Any bug
 * in move generation or make/unmake (castling, en passant, promotions, pins, checks) changes these counts.
 *
 * @see <a href="https://www.chessprogramming.org/Perft_Results">Chess Programming Wiki - Perft Results</a>
 */
class PerftTest {

    @ParameterizedTest(name = "{0} at depth {1}")
    @CsvSource(delimiter = ';', value = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; 4; 197281",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1; 3; 97862",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1; 5; 674624",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1; 4; 422333",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8; 3; 62379",
    })
    void perftMatchesKnownNodeCounts(String fen, int depth, long expectedNodes) {
        assertEquals(expectedNodes, perft(new MoveGenerator(), FEN.toBoard(fen), depth));
    }

    private long perft(MoveGenerator generator, Board board, int depth) {
        List<Move> moves = new ArrayList<>(generator.generateMoves(board));
        if (depth == 1) return moves.size();
        long nodes = 0;
        for (Move move : moves) {
            board.makeMove(move);
            nodes += perft(generator, board, depth - 1);
            board.unmakeMove();
        }
        return nodes;
    }
}
