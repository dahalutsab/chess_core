/**
 * Author: Utsab Dahal
 * User:LEGION
 * Date:3/10/2025
 * Time:2:06 PM
 */

package com.utsab.chess_engine.util;

import com.utsab.chess_engine.engine.Bitboard;

public class FenParser {
    public static Bitboard fromFEN(String fen) {
        Bitboard board = new Bitboard();
        String[] parts = fen.split(" ");
        String position = parts[0];

        int square = 56; // Start from A8
        for (char c : position.toCharArray()) {
            if (Character.isDigit(c)) {
                square += Character.getNumericValue(c);
            } else if (c == '/') {
                square -= 16;
            } else {
                board.setPiece(square);
                square++;
            }
        }
        return board;
    }
}
