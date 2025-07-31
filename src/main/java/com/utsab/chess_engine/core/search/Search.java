package com.utsab.chess_engine.core.search;

import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.transposition.TranspositionTable;

import java.time.Duration;

/**
 * Defines the contract for chess engine search algorithms in a UCI-compatible context. Implementations find the best
 * move and evaluation within a given time limit, leveraging transposition tables and multi-threading.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 */
public interface Search {

    /**
     * Sets the board position for the search.
     *
     * @param board the board position
     * @throws NullPointerException if board is null
     * @author Utsab Dahal
     */
    void setPosition(Board board);

    /**
     * Sets the size of the transposition table.
     *
     * @param hashSizeMb the size in megabytes
     * @throws IllegalArgumentException if hashSizeMb is negative
     * @author Utsab Dahal
     */
    void setHashSize(int hashSizeMb);

    /**
     * Sets the number of threads for parallel search.
     *
     * @param threadCount the number of threads
     * @throws IllegalArgumentException if threadCount is non-positive
     * @author Utsab Dahal
     */
    void setThreadCount(int threadCount);

    /**
     * Searches for the best move within the specified time limit.
     *
     * @param duration the search duration
     * @return the search result containing the best move and evaluation
     * @throws NullPointerException if duration is null
     */
    SearchResult negamax(Duration duration);

    /**
     * Retrieves the transposition table used by the search.
     *
     * @return the transposition table
     */
    TranspositionTable getTranspositionTable();

    /**
     * Clears cached search data, including transposition and move ordering tables.
     */
    void clearHistory();

    /**
     * Logs current search statistics.
     */
    default void logStatistics() {
        // No-op by default; implementations may override
    }
}