package com.utsab.chess_engine.move.entity;

import com.utsab.chess_engine.common.Auditable;
import com.utsab.chess_engine.game.entity.Game;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Represents a single move in a chess game, storing move notation and move number.
 * Extends Auditable to include lifecycle tracking fields.
 * The board state (FEN or Zobrist key) can be derived from the game's initial FEN and prior moveEntities.
 *
 * @author Utsab Dahal
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "moves")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MoveEntity extends Auditable {

    /**
     * Unique identifier for the move.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /**
     * The game to which this move belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    Game game;

    /**
     * The move number (e.g., 1.0 for White's first move, 1.5 for Black's first move).
     */
    @Column(name = "move_number", nullable = false)
    float moveNumber;

    /**
     * The move in standard algebraic notation (e.g., "e4", "Nf3", "O-O").
     */
    @Column(name = "uci", nullable = false)
    String uci;
}