package com.utsab.chess_engine.chat;

import com.utsab.chess_engine.game.entity.Game;
import com.utsab.chess_engine.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** A chat message between the players of a game */
@Entity
@Table(name = "game_messages", indexes = @Index(columnList = "game_id, created_at"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {

    public static final int MAX_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private UserEntity sender;

    @Column(nullable = false, length = MAX_LENGTH)
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
