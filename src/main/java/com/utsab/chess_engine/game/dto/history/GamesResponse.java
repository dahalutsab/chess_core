package com.utsab.chess_engine.game.dto.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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
    private Integer moveCount;
    private String playerColor;
    private BotResponse bot;

    public GamesResponse(String string, String name, String playerName, String playerName1, String string1, String endTime, String name1, int size, String playerColor, BotResponse botResponse) {
        this.gameId = string;
        this.gameType = name;
        this.whitePlayer = playerName;
        this.blackPlayer = playerName1;
        this.startTime = string1;
        this.endTime = endTime;
        this.result = name1;
        this.moveCount = size;
        this.playerColor = playerColor;
        this.bot = botResponse;
    }
}
