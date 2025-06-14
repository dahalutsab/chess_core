package com.utsab.chess_engine.bot.entity;

import com.utsab.chess_engine.common.Auditable;
import com.utsab.chess_engine.common.entity.Player;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Entity
@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Builder
public class Bots extends Player {

    private String botName;

    private String botDescription;

    private String personality;

    private String botImageUrl;

    private String slug;

    private int maxDepth;

    @ElementCollection
    private List<String> trashTalks;

    private int eloRating;

}
