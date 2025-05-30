package com.utsab.chess_engine.bot.dto;

import com.utsab.chess_engine.bot.entity.Bots;
import com.utsab.chess_engine.util.file.FileUrlUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BotsResponse {
    private String name;

    private String description;

    private String personality;

    private URI avatar;

    private String slug;

    private List<String> trashTalks;

    private Integer rating;

    public BotsResponse(Bots bots) {
        this.name = bots.getBotName();
        this.description = bots.getBotDescription();
        this.personality = bots.getPersonality();
        this.avatar = FileUrlUtil.getFileUri(bots.getBotImageUrl());
        this.slug = bots.getSlug();
        this.trashTalks = bots.getTrashTalks();
        this.rating = bots.getEloRating();
    }
}
