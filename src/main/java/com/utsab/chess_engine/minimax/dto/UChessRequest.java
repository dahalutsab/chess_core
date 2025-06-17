package com.utsab.chess_engine.minimax.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UChessRequest {
    private UUID gameId;
    private String move;
}
