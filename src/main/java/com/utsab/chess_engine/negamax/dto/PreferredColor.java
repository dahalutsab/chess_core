package com.utsab.chess_engine.negamax.dto;

public enum PreferredColor {
    WHITE,
    BLACK,
    RANDOM;

    public static PreferredColor fromString(String color) {
        if (color == null) {
            return RANDOM;
        }
        return switch (color.toUpperCase()) {
            case "WHITE" -> WHITE;
            case "BLACK" -> BLACK;
            default -> RANDOM;
        };
    }
}
