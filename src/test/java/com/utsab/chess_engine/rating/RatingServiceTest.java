package com.utsab.chess_engine.rating;

import com.utsab.chess_engine.user.entity.UserEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RatingServiceTest {

    @Test
    void evenPlayersGainAndLoseHalfTheKFactor() {
        assertEquals(20, RatingService.change(1200, 1200, 1, 40));
        assertEquals(-20, RatingService.change(1200, 1200, 0, 40));
        assertEquals(0, RatingService.change(1200, 1200, 0.5, 40));
    }

    @Test
    void beatingAStrongerPlayerIsWorthMore() {
        int upset = RatingService.change(1200, 1600, 1, 20);
        int expectedWin = RatingService.change(1600, 1200, 1, 20);
        assertTrue(upset > 15, "upset gain " + upset);
        assertTrue(expectedWin < 5, "expected-win gain " + expectedWin);
        // Elo is zero-sum between two players with the same K-factor
        assertEquals(0, upset + RatingService.change(1600, 1200, 0, 20));
    }

    @Test
    void kFactorDropsOnceARatingIsEstablished() {
        UserEntity newcomer = new UserEntity();
        assertEquals(40, RatingService.kFactor(newcomer));

        UserEntity established = new UserEntity();
        established.setRatedGames(RatingService.PROVISIONAL_GAMES);
        assertEquals(20, RatingService.kFactor(established));

        established.setRating(2450);
        assertEquals(10, RatingService.kFactor(established));
    }
}
