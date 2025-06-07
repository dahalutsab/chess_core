package com.utsab.chess_engine.minimax.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UChessResponse {
    private String gameId;
    private String fen;
    private String bestMove;
    private String bestMoveSan;
}
