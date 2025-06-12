package com.utsab.chess_engine.core.chess.search;

import com.utsab.chess_engine.core.chess.Core;
import com.utsab.chess_engine.core.chess.evaluation.Score;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Manages synchronization and logging of search results across multiple threads in a UCI-compatible chess engine.
 * Ensures that search results are reported only when a deeper depth is reached or a checkmate is found.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 */
@Component
public class ThreadManager {

    private int depthLogged;

    /**
     * Initializes a ThreadManager with zero depth logged.
     */
    public ThreadManager() {
        depthLogged = 0;
    }

    /**
     * Processes a search result, logging it if it exceeds the current logged depth or indicates a checkmate.
     *
     * @param searchResult the search result to process
     * @throws NullPointerException if searchResult is null
     */
    public synchronized void handleSearchResult(SearchResult searchResult) {
        Objects.requireNonNull(searchResult, "Search result cannot be null");
        if (searchResult.depth() > depthLogged
                && (searchResult.time() > 0 || Score.isMateScore(searchResult.eval()))) {
            Core.writeSearchInfo(searchResult);
            depthLogged++;
        }
    }

    /**
     * Resets the logged depth to zero.
     */
    public synchronized void reset() {
        depthLogged = 0;
    }
}