package com.utsab.chess_engine.game.entity;

import com.utsab.chess_engine.common.Auditable;
import com.utsab.chess_engine.common.entity.Player;
import com.utsab.chess_engine.game.entity.enums.GameResult;
import com.utsab.chess_engine.game.entity.enums.GameType;
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

    /** {@link #termination} of a game a player resigned */
    public static final String RESIGNATION = "RESIGNATION";
    /** {@link #termination} of a game the player to move left without finishing */
    public static final String ABANDONED = "ABANDONED";
    /**
     * {@link #termination} of a game that was left before both sides moved. It counts for nobody: it is unrated and
     * left out of statistics. (The result still names a winner, as the result column has no value for it.)
     */
    public static final String ABORTED = "ABORTED";

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
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    // Without an explicit order the database returns rows in storage order, which changes when rows are rewritten,
    // and the moves would be replayed out of order
    @OrderBy("id ASC")
    @NotNull
    List<MoveEntity> moveEntities = new ArrayList<>();


    @Column @Enumerated(EnumType.STRING)
    GameType gameType = GameType.PVB;

    /**
     * How the game ended when it isn't implied by the result, e.g. {@link com.utsab.chess_engine.game.clock.GameClock#TIMEOUT}.
     * Kept separate from {@link GameResult} so that existing databases (whose result column has a check constraint
     * listing the enum values) need no migration.
     */
    @Column(name = "termination")
    String termination;

    // Clock state for timed games; all null for untimed games. See GameClock.
    @Column(name = "base_time_ms")
    Long baseTimeMs;

    @Column(name = "increment_ms")
    Long incrementMs;

    @Column(name = "white_time_ms")
    Long whiteTimeMs;

    @Column(name = "black_time_ms")
    Long blackTimeMs;

    /** When the side to move's clock last started, in epoch milliseconds. */
    @Column(name = "clock_started_at")
    Long clockStartedAt;

    // Elo ratings at the start of the game and how much each changed when it ended; all null until a rated game ends.
    // A bot's rating is fixed, so its change is always 0.
    @Column(name = "white_rating_before")
    Integer whiteRatingBefore;

    @Column(name = "black_rating_before")
    Integer blackRatingBefore;

    @Column(name = "white_rating_change")
    Integer whiteRatingChange;

    @Column(name = "black_rating_change")
    Integer blackRatingChange;

    /** "WHITE" or "BLACK" while that side's draw offer awaits an answer */
    @Column(name = "draw_offered_by")
    String drawOfferedBy;

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