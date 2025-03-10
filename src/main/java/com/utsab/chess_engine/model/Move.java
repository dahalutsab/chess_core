/**
 * Author: Utsab Dahal
 * User:LEGION
 * Date:3/10/2025
 * Time:2:11 PM
 */

package com.utsab.chess_engine.model;


import lombok.Getter;

@Getter
public class Move {
    private final int fromSquare;
    private final int toSquare;
    private final char promotion; // For pawn promotions (Q, R, B, N)

    public Move(int fromSquare, int toSquare, char promotion) {
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.promotion = promotion;
    }

    @Override
    public String toString() {
        return "Move{" + fromSquare + " -> " + toSquare + " " + (promotion != '-' ? promotion : "") + "}";
    }
}
