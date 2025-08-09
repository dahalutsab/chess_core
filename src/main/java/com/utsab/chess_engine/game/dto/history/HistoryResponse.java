package com.utsab.chess_engine.game.dto.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoryResponse {
    private GameStatsResponse gameStats;
    private Page<GamesResponse> games;
}
