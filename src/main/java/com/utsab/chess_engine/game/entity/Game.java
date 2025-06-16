package com.utsab.chess_engine.game.entity;

import com.utsab.chess_engine.common.Auditable;
import com.utsab.chess_engine.common.entity.Player;
import com.utsab.chess_engine.game.entity.enums.GameResult;
import com.utsab.chess_engine.move.entity.MoveEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a chess game, including metadata about players, game state, and result.
 * Extends Auditable to include lifecycle tracking fields.
 * Supports both player vs. bot and player vs. player scenarios, with history tracking.
 *
 * @author Utsab Dahal
 */
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Game extends Auditable {

    /**
     * Unique identifier for the game.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    /**
     * The player (user or bot) playing as White.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_player_id", nullable = false)
    Player whitePlayer;

    /**
     * The player (user or bot) playing as Black.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_player_id", nullable = false)
    Player blackPlayer;

    /**
     * The initial FEN position of the game. Defaults to the standard chess starting position.
     */
    @Column(name = "initial_fen", nullable = false)
    String initialFen = "rnbqkbnr/pppppppp/5n1f/8/8/5N1F/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /**
     * The start time of the game.
     */
    @Column(name = "start_time", nullable = false)
    LocalDateTime startTime;

    /**
     * The end time of the game, if completed.
     */
    @Column(name = "end_time")
    LocalDateTime endTime;

    /**
     * The result of the game (e.g., WHITE_WIN, BLACK_WIN, DRAW, IN_PROGRESS).
     */
    @Column(name = "result", nullable = false)
    @Enumerated(EnumType.STRING)
    GameResult result = GameResult.IN_PROGRESS;

    /**
     * The list of moveEntities played in the game, ordered by move number.
     */
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @NotNull
    List<MoveEntity> moveEntities = new ArrayList<>();

    /**
     * Returns the list of moves in the game, guaranteed to be non-null.
     *
     * @return the list of move entities
     * @author Utsab Dahal
     */
    @NotNull
    public List<MoveEntity> getMoveEntities() {
        if (moveEntities == null) {
            moveEntities = new ArrayList<>();
        }
        return moveEntities;
    }
}