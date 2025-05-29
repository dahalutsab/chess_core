/**
 * Author: Utsab Dahal
 * User:LEGION
 * Date:3/20/2025
 * Time:3:10 PM
 */

package com.utsab.chess_engine.decision.ai.random.response;

public class MoveResponse {
    private String bestMove;

    public MoveResponse(String bestMove) {
        this.bestMove = bestMove;
    }

    public String getBestMove() { return bestMove; }
    public void setBestMove(String bestMove) { this.bestMove = bestMove; }
}