package com.utsab.chess_engine.minimax.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UChessResponse {
    private UUID id;
    private String fen;
    private String bestMove;
    private String bestMoveSan;
    private String result;
}
