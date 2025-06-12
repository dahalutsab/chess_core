package com.utsab.chess_engine.core.chess.search;

import com.utsab.chess_engine.core.chess.board.Move;

/**
 * Represents the result of a chess engine search, encapsulating the evaluation, best move, search depth, time taken,
 * nodes evaluated, and nodes per second (NPS). Used in a UCI-compatible chess engine to report search outcomes.
 *
 * @param eval  the evaluation score (positive favors White, negative favors Black)
 * @param move  the best move found, or null if no move is selected
 * @param depth the search depth reached (in plies)
 * @param time  the time taken for the search (in milliseconds)
 * @param nodes the number of nodes evaluated
 * @param nps   the nodes per second
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 */
public record SearchResult(int eval, Move move, int depth, long time, int nodes, long nps) {

    /**
     * Constructs a SearchResult with validation for non-negative values.
     *
     * @throws IllegalArgumentException if depth, time, nodes, or nps is negative
     */
    public SearchResult {
        if (depth < 0) throw new IllegalArgumentException("Depth cannot be negative: " + depth);
        if (time < 0) throw new IllegalArgumentException("Time cannot be negative: " + time);
        if (nodes < 0) throw new IllegalArgumentException("Nodes cannot be negative: " + nodes);
        if (nps < 0) throw new IllegalArgumentException("NPS cannot be negative: " + nps);
    }
}