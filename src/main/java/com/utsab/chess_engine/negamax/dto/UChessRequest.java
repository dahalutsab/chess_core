package com.utsab.chess_engine.negamax.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    /** How long the player took over this move, as measured by the client; used to credit back network delay */
    private Long thinkTimeMs;
    /** When the request reached the server (set by the server, not the client) */
    @JsonIgnore
    private Long receivedAt;

    public UChessRequest(UUID gameId, String move) {
        this.gameId = gameId;
        this.move = move;
    }
}
