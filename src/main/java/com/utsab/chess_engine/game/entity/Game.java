package com.utsab.chess_engine.game.entity;

import com.utsab.chess_engine.bot.entity.Bots;
import com.utsab.chess_engine.common.Auditable;
import com.utsab.chess_engine.game.entity.enums.GameResult;
import com.utsab.chess_engine.game.entity.enums.GameType;
import com.utsab.chess_engine.move.entity.Move;
import com.utsab.chess_engine.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game extends Auditable {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private UserEntity whitePlayer; // Can be null if bot

    @ManyToOne
    private UserEntity blackPlayer;

    @ManyToOne
    private Bots whiteBot; // if playing against bot

    @ManyToOne
    private Bots blackBot;

    @Enumerated(EnumType.STRING)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    private GameResult result;

    private String finalFEN;

    private int totalMoves;

    private long durationMillis;

    private boolean isRated = false;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<Move> moves = new ArrayList<>();
}
