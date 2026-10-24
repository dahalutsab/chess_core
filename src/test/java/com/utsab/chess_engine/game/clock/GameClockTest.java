package com.utsab.chess_engine.game.clock;

import com.utsab.chess_engine.game.entity.Game;
import com.utsab.chess_engine.game.entity.enums.GameResult;
import com.utsab.chess_engine.move.entity.MoveEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameClockTest {

    /** A timed game whose clock is running, with the side to move's clock started at {@code startedAt} */
    private static Game runningGame(long startedAt) {
        Game game = new Game();
        game.setResult(GameResult.IN_PROGRESS);
        game.setMoveEntities(new ArrayList<>(List.of(new MoveEntity(), new MoveEntity())));
        GameClock.start(game, new GameClock.TimeControl(300_000, 0), startedAt);
        return game;
    }

    @Test
    void networkDelayIsCreditedBack() {
        Game game = runningGame(10_000);
        // Arrived 3 s after the clock started; the player says they took 2.5 s, so 0.5 s was network delay
        assertEquals(12_500, GameClock.moveTime(game, 13_000L, 2_500L, 20_000));
    }

    @Test
    void creditIsCappedPerMove() {
        Game game = runningGame(10_000);
        // A client claiming to have taken no time at all gains at most the cap
        assertEquals(13_000 - GameClock.MAX_LAG_COMPENSATION_MS, GameClock.moveTime(game, 13_000L, 0L, 20_000));
    }

    @Test
    void reportedTimeCannotExceedTheMeasuredTime() {
        Game game = runningGame(10_000);
        assertEquals(13_000, GameClock.moveTime(game, 13_000L, 9_999L, 20_000));
    }

    @Test
    void withoutAReportTheArrivalTimeCounts() {
        Game game = runningGame(10_000);
        assertEquals(13_000, GameClock.moveTime(game, 13_000L, null, 20_000));
        // and without an arrival time, the current time
        assertEquals(20_000, GameClock.moveTime(game, null, null, 20_000));
    }

    @Test
    void moveTimeIsArrivalBeforeTheClockRuns() {
        Game game = runningGame(10_000);
        game.setMoveEntities(new ArrayList<>());
        assertEquals(13_000, GameClock.moveTime(game, 13_000L, 0L, 20_000));
    }

    @Test
    void restartMovesTheClockStartForward() {
        Game game = runningGame(10_000);
        GameClock.restart(game, 12_000);
        assertEquals(12_000, game.getClockStartedAt());
        // never backwards
        GameClock.restart(game, 11_000);
        assertEquals(12_000, game.getClockStartedAt());
    }
}
