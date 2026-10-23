package com.utsab.chess_engine.core.evaluation;

import com.utsab.chess_engine.core.board.BitOperations;
import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.GameState;
import com.utsab.chess_engine.core.search.Search;


public class Result {

    /**
     * Check for an 'effective' draw, which treats a single repetition of the position as a draw.
     * This is used during {@link Search} to quickly check for a draw; it will lead to some errors in edge cases, but the
     * gamble is that the boost in search speed is worth the potential cost.
     */
    public static boolean isEffectiveDraw(Board board) {
        return isDoubleRepetition(board) || isFiftyMoveRule(board) || isInsufficientMaterial(board);
    }

    public static boolean isDoubleRepetition(Board board) {
        return countRepetitions(board, 1) >= 1;
    }

    /**
     * Check for a draw by threefold repetition, i.e. the current position has occurred at least twice before. This is
     * the actual rule of the game, and should be used to adjudicate game results.
     */
    public static boolean isThreefoldRepetition(Board board) {
        return countRepetitions(board, 2) >= 2;
    }

    /**
     * Count the previous occurrences of the current position, stopping once {@code limit} have been found. Only
     * positions since the last capture or pawn move can repeat, and only every second ply has the same side to move,
     * so the scan is bounded by the fifty-move counter rather than the full game history.
     */
    private static int countRepetitions(Board board, int limit) {
        long zobrist = board.getGameState().getZobristKey();
        int reversiblePlies = board.getGameState().getFiftyMoveCounter();
        int count = 0;
        int pliesAgo = 0;
        // The history is a stack, so iteration starts from the most recent position
        for (GameState gameState : board.getGameStateHistory()) {
            pliesAgo++;
            if (pliesAgo > reversiblePlies) break;
            if ((pliesAgo & 1) == 0 && gameState.getZobristKey() == zobrist && ++count >= limit) break;
        }
        return count;
    }

    public static boolean isInsufficientMaterial(Board board) {
        if (board.getWhitePawns() != 0 || board.getWhiteRooks() != 0 || board.getWhiteQueens() != 0
                || board.getBlackPawns() != 0 || board.getBlackRooks() != 0 || board.getBlackQueens() != 0) {
            return false;
        }
        long whitePieces = board.getWhiteKnights() | board.getWhiteBishops();
        long blackPieces = board.getBlackKnights() |  board.getBlackBishops();

        return (BitOperations.countBits(whitePieces) == 0 || BitOperations.countBits(whitePieces) == 1)
                && (BitOperations.countBits(blackPieces) == 0 || BitOperations.countBits(blackPieces) == 1);
    }

    public static boolean isFiftyMoveRule(Board board) {
        return board.getGameState().getFiftyMoveCounter() >= 100;
    }


}
