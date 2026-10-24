package com.utsab.chess_engine.game;

import com.utsab.chess_engine.common.ActivityTracker;
import com.utsab.chess_engine.negamax.service.UChessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

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

    /** The sweep pauses after this long without real requests, letting an idle database sleep */
    private static final Duration PAUSE_WHEN_IDLE_FOR = Duration.ofMinutes(20);

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void sweep() {
        // Games left while the app is idle are ended on the first sweep after someone returns
        if (ActivityTracker.idleFor().compareTo(PAUSE_WHEN_IDLE_FOR) > 0) return;
        try {
            int ended = gameService.sweepAbandonedGames();
            if (ended > 0) log.info("[AbandonedGameSweeper] Ended {} abandoned games", ended);
        } catch (RuntimeException e) {
            log.error("[AbandonedGameSweeper] Sweep failed", e);
        }
    }
}
