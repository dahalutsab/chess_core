package com.utsab.chess_engine.core.chess.board;

import lombok.Getter;

/**
 * Enumerates chess piece types, each with an index for internal representation and a material value for evaluation.
 * Used throughout the chess engine to identify pieces and assess their worth in a UCI-compatible context.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 */
@Getter
public enum Piece {

    /**
     * Pawn piece, with a material value of 100.
     */
    PAWN(0, 100),

    /**
     * Knight piece, with a material value of 320.
     */
    KNIGHT(1, 320),

    /**
     * Bishop piece, with a material value of 330.
     */
    BISHOP(2, 330),

    /**
     * Rook piece, with a material value of 500.
     */
    ROOK(3, 500),

    /**
     * Queen piece, with a material value of 900.
     */
    QUEEN(4, 900),

    /**
     * King piece, with a material value of 0 (not traded).
     */
    KING(5, 0);

    /**
     * The index of the piece for internal representation (0-5).
     */
    private final int index;

    /**
     * The material value of the piece for evaluation purposes.
     */
    private final int value;

    /**
     * Constructs a piece with the specified index and material value.
     *
     * @param index the internal index of the piece (0-5)
     * @param value the material value for evaluation
     */
    Piece(int index, int value) {
        this.index = index;
        this.value = value;
    }

    /**
     * Checks if the piece is a sliding piece (bishop, rook, or queen).
     *
     * @return true if the piece is a slider, false otherwise
     */
    public boolean isSlider() {
        return switch (this) {
            case BISHOP, ROOK, QUEEN -> true;
            default -> false;
        };
    }
}