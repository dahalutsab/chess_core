/**
 * Author: Utsab Dahal
 * User:LEGION
 * Date:3/15/2025
 * Time:8:19 PM
 */

package com.utsab.chess_engine.board;

public record Move(int fromSquare, int toSquare) {

    @Override
    public String toString() {
        return String.format("%s%s", squareToString(fromSquare), squareToString(toSquare));
    }

    private String squareToString(int square) {
        char file = (char) ('a' + (square % 8));
        int rank = 8 - (square / 8);
        return String.format("%c%d", file, rank);
    }
}