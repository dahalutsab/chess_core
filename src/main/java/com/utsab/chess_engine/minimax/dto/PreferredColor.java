package com.utsab.chess_engine.minimax.dto;

public enum PreferredColor {
    WHITE,
    BLACK,
    DEFAULT;

    public static PreferredColor fromString(String color) {
        if (color == null) {
            return DEFAULT;
        }
        return switch (color.toUpperCase()) {
            case "WHITE" -> WHITE;
            case "BLACK" -> BLACK;
            default -> DEFAULT;
        };
    }
}
