package com.utsab.chess_engine.game.entity.enums;

import lombok.Getter;

/**
 * Represents the possible outcomes of a chess game.
 *
 * @author Utsab Dahal
 */
@Getter
public enum GameResult {
    WHITE_WIN("1-0"),
    BLACK_WIN("0-1"),
    DRAW("1/2-1/2"),
    IN_PROGRESS("*");

    private final String notation;

    GameResult(String notation) {
        this.notation = notation;
    }
}