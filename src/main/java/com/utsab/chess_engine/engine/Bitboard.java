/**
 * Author: Utsab Dahal
 * User:LEGION
 * Date:3/10/2025
 * Time:2:05 PM
 */

package com.utsab.chess_engine.engine;

public class Bitboard {
    private long boardState;

    public Bitboard() {
        this.boardState = 0L; // Empty board
    }

    public void setPiece(int square) {
        boardState |= (1L << square);
    }

    public void clearPiece(int square) {
        boardState &= ~(1L << square);
    }

    public boolean isOccupied(int square) {
        return (boardState & (1L << square)) != 0;
    }

    public long getBoardState() {
        return boardState;
    }

}
