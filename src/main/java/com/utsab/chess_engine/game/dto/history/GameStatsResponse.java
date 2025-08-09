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
}
