package com.utsab.chess_engine.game.entity.enums;

import lombok.Getter;

/**
 * Represents the possible outcomes of a chess game.
 *
 * @author Utsab Dahal
 */
@Getter
public enum GameResult {
    WHITE_WIN("1-0", "White wins by checkmate"),
    BLACK_WIN("0-1", "Black wins by checkmate"),
    DRAW_BY_STALEMATE("1/2-1/2", "Draw by stalemate"),
    DRAW_BY_REPETITION("1/2-1/2", "Draw by repetition"),
    DRAW_BY_FIFTY_MOVE_RULE("1/2-1/2", "Draw by fifty move rule"),
    DRAW_BY_INSUFFICIENT_MATERIAL("1/2-1/2", "Draw by insufficient material"),
    DRAW_BY_AGREEMENT("1/2-1/2", "Draw by mutual agreement"),
    IN_PROGRESS("*", "Game in progress");

    private final String notation;
    private final String description;

    GameResult(String notation, String description) {
        this.notation = notation;
        this.description = description;
    }

    public boolean isDraw() {
        return this == DRAW_BY_STALEMATE
                || this == DRAW_BY_REPETITION
                || this == DRAW_BY_FIFTY_MOVE_RULE
                || this == DRAW_BY_INSUFFICIENT_MATERIAL
                || this == DRAW_BY_AGREEMENT;
    }

    public boolean isFinished() {
        return this != IN_PROGRESS;
    }
}