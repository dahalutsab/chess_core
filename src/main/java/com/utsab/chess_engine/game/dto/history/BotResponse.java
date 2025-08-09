package com.utsab.chess_engine.game.dto.history;

import com.utsab.chess_engine.bot.entity.Bots;
import com.utsab.chess_engine.util.file.FileUrlUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BotResponse {
    private String name;

    private URI avatar;


    public BotResponse(Bots bots) {
        this.name = bots.getBotName();
        this.avatar = FileUrlUtil.getFileUri(bots.getBotImageUrl());
    }
}
