package com.utsab.chess_engine.core.chess.search;

import com.utsab.chess_engine.core.chess.Core;
import com.utsab.chess_engine.core.chess.evaluation.Score;

public class ThreadManager {

    int depthLogged;

    public ThreadManager() {
        depthLogged = 0;
    }

    public synchronized void handleSearchResult(SearchResult searchResult) {
        if (searchResult.depth() > depthLogged
                && (searchResult.time() > 0 || Score.isMateScore(searchResult.eval()))) {
            Core.writeSearchInfo(searchResult);
            depthLogged++;
        }
    }

    public synchronized void reset() {
        depthLogged = 0;
    }

}
