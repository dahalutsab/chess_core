package com.utsab.chess_engine.game.dto.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GamesResponse {
    private String gameId;
    private String gameType;
    private String whitePlayer;
    private String blackPlayer;
    private String startTime;
    private String endTime;
    private String result;
    /** How the game ended when not implied by the result, e.g. TIMEOUT or RESIGNATION */
    private String termination;
    private Integer moveCount;
    private String playerColor;
    /** Set when the opponent is a bot */
    private BotResponse bot;
    private String opponentName;
    private URI opponentAvatar;
    /** The opponent's rating when the game started */
    private Integer opponentRating;
    /** How much the user's rating changed; null while the game is in progress */
    private Integer ratingChange;
    /** The user's rating after the game, or at its start while it is in progress */
    private Integer rating;
}
