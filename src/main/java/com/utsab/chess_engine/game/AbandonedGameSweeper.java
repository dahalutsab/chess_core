package com.utsab.chess_engine.game;

import com.utsab.chess_engine.negamax.service.UChessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically ends games that were left unfinished, so they get a result, are rated and stop showing as in progress.
 *
 * @author Utsab Dahal
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedGameSweeper {

    private final UChessService gameService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void sweep() {
        try {
            int ended = gameService.sweepAbandonedGames();
            if (ended > 0) log.info("[AbandonedGameSweeper] Ended {} abandoned games", ended);
        } catch (RuntimeException e) {
            log.error("[AbandonedGameSweeper] Sweep failed", e);
        }
    }
}
