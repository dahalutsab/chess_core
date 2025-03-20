package com.utsab.chess_engine.board;

import java.util.HashMap;
import java.util.Map;

public class FenParser {
    private FenParser() {}
    private static final Map<Character, Integer> PIECE_INDEX = new HashMap<>();

    static {
        // White pieces
        PIECE_INDEX.put('P', 0);
        PIECE_INDEX.put('N', 1);
        PIECE_INDEX.put('B', 2);
        PIECE_INDEX.put('R', 3);
        PIECE_INDEX.put('Q', 4);
        PIECE_INDEX.put('K', 5);
        // Black pieces
        PIECE_INDEX.put('p', 6);
        PIECE_INDEX.put('n', 7);
        PIECE_INDEX.put('b', 8);
        PIECE_INDEX.put('r', 9);
        PIECE_INDEX.put('q', 10);
        PIECE_INDEX.put('k', 11);
    }

    public static void loadFromFen(Bitboard bitboard, String fen) {
        String[] parts = fen.split(" ");
        String[] ranks = parts[0].split("/");

        for (int rank = 0; rank < 8; rank++) {
            int file = 0;
            for (char c : ranks[rank].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += Character.getNumericValue(c);
                } else {
                    int square = rank * 8 + file;
                    bitboard.setPiece(PIECE_INDEX.get(c), square);
                    file++;
                }
            }
        }
    }
}