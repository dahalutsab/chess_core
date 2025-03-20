package com.utsab.chess_engine.board;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class FenParser {
    private static final Logger logger = Logger.getLogger(FenParser.class.getName());
    private static final Map<Character, Integer> PIECE_INDEX = new HashMap<>();

    static {
        PIECE_INDEX.put('P', 0); PIECE_INDEX.put('N', 1); PIECE_INDEX.put('B', 2);
        PIECE_INDEX.put('R', 3); PIECE_INDEX.put('Q', 4); PIECE_INDEX.put('K', 5);
        PIECE_INDEX.put('p', 6); PIECE_INDEX.put('n', 7); PIECE_INDEX.put('b', 8);
        PIECE_INDEX.put('r', 9); PIECE_INDEX.put('q', 10); PIECE_INDEX.put('k', 11);
    }

    public static Bitboard loadFromFen(String fen, MoveGenerator moveGenerator) {
        Bitboard bitboard = new Bitboard();
        String[] parts = fen.split(" ");
        String[] ranks = parts[0].split("/");
        moveGenerator.setWhiteToMove(parts[1].equals("w"));

        int square = 56; // Start at a8 (rank 8, file a)
        for (String rank : ranks) {
            for (char c : rank.toCharArray()) {
                if (Character.isDigit(c)) {
                    square += Character.getNumericValue(c);
                } else {
                    bitboard.setPiece(PIECE_INDEX.get(c), square);
                    square++;
                }
            }
            square -= 16; // Move to next rank
        }
        logger.info("Loaded FEN: " + fen);
        bitboard.logPosition();
        return bitboard;
    }
}