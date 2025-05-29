/**
 * Author: Utsab Dahal
 * User:LEGION
 * Date:3/20/2025
 * Time:3:10 PM
 */

package com.utsab.chess_engine.decision.ai.minimax.response;

public class MiniMaxResponse {
    private String bestMove;
    private double score;
    private int depth;

    public MiniMaxResponse(String bestMove, double score, int depth) {
        this.bestMove = bestMove;
        this.score = score;
        this.depth = depth;
    }

    public String getBestMove() { return bestMove; }
    public void setBestMove(String bestMove) { this.bestMove = bestMove; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
}