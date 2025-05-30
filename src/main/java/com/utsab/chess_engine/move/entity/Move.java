package com.utsab.chess_engine.move.entity;

import com.utsab.chess_engine.game.entity.Game;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "moves")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Move {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Game game;

    private int moveNumber;

    private String fromSquare; // e.g., "e2"
    private String toSquare;   // e.g., "e4"

    private String san;        // e.g., "Nf3", "O-O", etc.
    private String fenAfter;   // optional

    private long moveTimeMillis; // Time taken for this move
}
