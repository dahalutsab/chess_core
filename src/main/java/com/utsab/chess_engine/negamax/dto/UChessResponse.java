package com.utsab.chess_engine.negamax.dto;

import com.utsab.chess_engine.game.dto.ClockDto;
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
    /** Clock state after the bot's move, or null for an untimed game */
    private ClockDto clock;
    /** How the game ended, e.g. "TIMEOUT", when not implied by the result */
    private String termination;
}
