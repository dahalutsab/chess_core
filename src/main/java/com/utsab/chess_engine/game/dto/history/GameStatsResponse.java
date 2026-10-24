package com.utsab.chess_engine.game.dto.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameStatsResponse {
    private Integer totalGames;
    private Integer totalWins;
    private Integer totalLosses;
    private Integer totalDraws;
    /** The player's current Elo rating */
    private Integer rating;
    /** Highest rating reached, from the ratings recorded on finished games */
    private Integer peakRating;
    private Integer ratedGames;
}
