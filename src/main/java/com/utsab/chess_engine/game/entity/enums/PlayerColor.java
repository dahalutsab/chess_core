package com.utsab.chess_engine.game.entity.enums;

import lombok.Getter;

/**
 * Represents the color of a player in a chess game.
 *
 * @author Utsab Dahal
 */
@Getter
public enum PlayerColor {
    WHITE("White"),
    BLACK("Black");

    private final String displayName;

    PlayerColor(String displayName) {
        this.displayName = displayName;
    }
}