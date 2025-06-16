package com.utsab.chess_engine.core.chess.search.moveordering;

import com.utsab.chess_engine.core.chess.board.Board;
import com.utsab.chess_engine.core.chess.board.Move;

import java.util.List;

/**
 * Defines a contract for ordering chess moveEntities based on heuristics to optimize search performance in a UCI-compatible
 * chess engine. Effective move ordering prioritizes likely best moveEntities (e.g., captures, promotions) to improve
 * alpha-beta pruning efficiency.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025-06-12
 * @see <a href="https://www.chessprogramming.org/Move_Ordering">Chess Programming Wiki - MoveEntity Ordering</a>
 */
public interface MoveOrdering  {

    /**
     * Orders a list of moveEntities using heuristics to prioritize moveEntities likely to be optimal, such as captures, promotions,
     * or previously successful moveEntities.
     *
     * @param board           the current board position
     * @param moves           the list of legal moveEntities to order
     * @param previousBestMove the best move from a previous search iteration, or null if none
     * @param includeKillers  whether to prioritize killer moveEntities
     * @param depth           the current search depth (ply)
     * @return a new list of moveEntities ordered by estimated quality (highest to lowest)
     * @throws NullPointerException if board or moveEntities is null
     */
    List<Move> orderMoves(Board board, List<Move> moves, Move previousBestMove, boolean includeKillers, int depth);

    /**
     * Assigns a numerical score to a move based on heuristics, such as MVV-LVA, killer moveEntities, or history scores.
     * Higher scores indicate moveEntities more likely to be optimal.
     *
     * @param board           the current board position
     * @param move            the move to score
     * @param previousBestMove the best move from a previous iteration, or null if none
     * @param includeKillers  whether to consider killer moveEntities in scoring
     * @param depth           the current search depth (ply)
     * @return the score representing the move's estimated quality
     * @throws NullPointerException if board or move is null
     */
    int scoreMove(Board board, Move move, Move previousBestMove, boolean includeKillers, int depth);

    /**
     * Computes a score for a move using the Most Valuable Victim - Least Valuable Aggressor (MVV-LVA) heuristic,
     * prioritizing captures of high-value pieces by low-value attackers.
     *
     * @param board           the current board position
     * @param move            the move to score
     * @param previousBestMove the best move from a previous iteration, or null if none
     * @return the MVV-LVA score, or 0 if the move is not a capture
     * @throws NullPointerException if board or move is null
     */
    int mvvLva(Board board, Move move, Move previousBestMove);

    /**
     * Registers a killer move for a given ply, typically a quiet move causing a beta cutoff.
     *
     * @param ply       the search depth (ply) at which the move is a killer
     * @param newKiller the move to register as a killer
     * @throws NullPointerException if newKiller is null
     */
    void addKillerMove(int ply, Move newKiller);

    /**
     * Updates the history heuristic table with a move that improved the search score.
     *
     * @param plyRemaining the remaining search depth
     * @param historyMove  the move to add to the history table
     * @param isWhite      true if the move was made by White
     * @throws NullPointerException if historyMove is null
     */
    void addHistoryMove(int plyRemaining, Move historyMove, boolean isWhite);

    /**
     * Clears all stored heuristic data, such as killer moveEntities and history tables.
     *
     */
    default void clear() {
        // No-op by default; implementations may override
    }
}