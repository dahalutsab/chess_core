package com.utsab.chess_engine.bot.entity;

import com.utsab.chess_engine.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Entity
@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Builder
public class Bots extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long botId;

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
