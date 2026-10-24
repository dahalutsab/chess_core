package com.utsab.chess_engine.social.entity;

import com.utsab.chess_engine.negamax.dto.PreferredColor;
import com.utsab.chess_engine.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An invitation from one user to another to play a game. Accepting it starts the game.
 *
 * @author Utsab Dahal
 */
@Entity
@Table(name = "challenges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Challenge {

    public enum Status { PENDING, ACCEPTED, DECLINED, CANCELLED }

    /** How long a challenge waits for an answer */
    public static final long EXPIRY_SECONDS = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenger_id")
    private UserEntity challenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opponent_id")
    private UserEntity opponent;

    /** "minutes" or "minutes|incrementSeconds", as for games against bots; null for untimed */
    @Column(name = "time_control")
    private String timeControl;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenger_color", nullable = false)
    private PreferredColor challengerColor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** The game started when the challenge was accepted */
    @Column(name = "game_id")
    private UUID gameId;

    public boolean isExpired() {
        return status == Status.PENDING && createdAt.isBefore(LocalDateTime.now().minusSeconds(EXPIRY_SECONDS));
    }
}
