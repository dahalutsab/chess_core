package com.utsab.chess_engine.core.chess.engine;

import com.utsab.chess_engine.core.chess.board.Board;
import com.utsab.chess_engine.core.chess.board.Move;
import com.utsab.chess_engine.core.chess.generation.MoveGeneration;
import com.utsab.chess_engine.core.chess.opening.OpeningBook;
import com.utsab.chess_engine.core.chess.search.Search;
import com.utsab.chess_engine.core.chess.search.SearchResult;
import com.utsab.chess_engine.core.chess.transposition.HashEntry;
import com.utsab.chess_engine.core.chess.transposition.TranspositionTable;
import com.utsab.chess_engine.core.chess.utils.notation.FEN;
import com.utsab.chess_engine.core.chess.utils.notation.Notation;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Manages the core functionality of a UCI-compatible chess engine. This class handles game state, board updates, move
 * generation, and search for the best move. It supports time-controlled games, pondering, and integration with an opening
 * book for early game moves.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Engine {

    EngineConfig config;
    OpeningBook book;
    MoveGeneration moveGenerator;
    Search searcher;
    CompletableFuture<SearchResult> think;
    Board board;

    /**
     * Constructs an Engine instance with the specified configuration, opening book, move generator, and search mechanism.
     *
     * @param config        the engine configuration settings
     * @param book          the opening book for early game moves
     * @param moveGenerator the move generation component
     * @param searcher      the search algorithm component
     */
    public Engine(EngineConfig config, OpeningBook book, MoveGeneration moveGenerator, Search searcher) {
        this.config = config;
        this.book = book;
        this.moveGenerator = moveGenerator;
        this.searcher = searcher;
    }

    /**
     * Initializes a new chess game by clearing the search history.
     *
     */
    public void newGame() {
        searcher.clearHistory();
    }

    /**
     * Sets the board position using a FEN string and applies a list of moves. Validates each move for legality before
     * applying it to the board.
     *
     * @param fen   the FEN string representing the board position
     * @param moves the list of moves to apply
     * @throws IllegalArgumentException if any move is illegal
     */
    public void setPosition(String fen, List<Move> moves) {
        board = FEN.toBoard(fen);
        for (Move move : moves) {
            Move legalMove = getLegalMove(move);
            board.makeMove(legalMove);
        }
        searcher.setPosition(board);
    }

    /**
     * Sets the board position directly and updates the search component.
     *
     * @param board the board to set as the current position
     */
    public void setPosition(Board board) {
        this.board = board;
        searcher.setPosition(board);
    }

    /**
     * Configures the size of the transposition table used by the search component.
     *
     * @param hashSizeMb the size of the transposition table in megabytes
     */
    public void setHashSize(int hashSizeMb) {
        this.searcher.setHashSize(hashSizeMb);
    }

    /**
     * Sets the number of threads used by the search component for parallel processing.
     *
     * @param threadCount the number of threads to use
     */
    public void setThreadCount(int threadCount) {
        this.searcher.setThreadCount(threadCount);
    }

    /**
     * Enables or disables the use of the engine's opening book.
     *
     * @param ownBookEnabled true to enable the opening book, false otherwise
     */
    public void setOwnBookEnabled(boolean ownBookEnabled) {
        this.config.setOwnBookEnabled(ownBookEnabled);
    }

    /**
     * Enables or disables pondering, allowing the engine to think during the opponent's turn.
     *
     * @param ponderEnabled true to enable pondering, false otherwise
     */
    public void setPonderEnabled(boolean ponderEnabled) {
        this.config.setPonderEnabled(ponderEnabled);
    }

    /**
     * Sets whether the current search should be cancelled.
     *
     * @param cancelled true to cancel the search, false otherwise
     */
    public void setSearchCancelled(boolean cancelled) {
        this.config.setSearchCancelled(cancelled);
    }

    /**
     * Sets whether the engine is in pondering mode.
     *
     * @param pondering true if the engine is pondering, false otherwise
     */
    public void setPondering(boolean pondering) {
        this.config.setPondering(pondering);
    }

    /**
     * Initiates an asynchronous search for the best move within the specified time limit. If an opening book move is
     * available, it is returned immediately. The result is passed to the provided callback.
     *
     * @param thinkTimeMs     the time limit for the search in milliseconds
     * @param onThinkComplete the callback to handle the search result
     */
    public void think(int thinkTimeMs, Consumer<SearchResult> onThinkComplete) {
        if (hasBookMove()) {
            Move bookMove = getLegalMove(book.getBookMove(board.getGameState().getZobristKey()));
            onThinkComplete.accept(new SearchResult(0, bookMove, 0, 0, 0, 0));
            return;
        }
        stopThinking();
        think = CompletableFuture.supplyAsync(() -> think(thinkTimeMs));
        think.thenAccept(onThinkComplete);
    }

    /**
     * Performs a synchronous search for the best move within the specified time limit.
     *
     * @param thinkTimeMs the time limit for the search in milliseconds
     * @return the search result containing the best move and evaluation
     */
    public SearchResult think(int thinkTimeMs) {
        Duration thinkTime = Duration.ofMillis(thinkTimeMs);
        return searcher.search(thinkTime);
    }

    /**
     * Checks if the engine is currently performing a search.
     *
     * @return true if a search is in progress, false otherwise
     */
    public boolean isThinking() {
        return think != null && !think.isDone();
    }

    /**
     * Cancels any ongoing search.
     *
     */
    public void stopThinking() {
        if (isThinking()) {
            think.cancel(true);
        }
    }

    /**
     * Calculates the optimal thinking time for the current player based on remaining time and increment.
     *
     * @param timeWhiteMs      the remaining time for White in milliseconds
     * @param timeBlackMs      the remaining time for Black in milliseconds
     * @param incrementWhiteMs the time increment for White in milliseconds
     * @param incrementBlackMs the time increment for Black in milliseconds
     * @return the calculated thinking time in milliseconds
     */
    public int chooseThinkTime(int timeWhiteMs, int timeBlackMs, int incrementWhiteMs, int incrementBlackMs) {
        boolean isWhite = board.isWhiteToMove();
        int timeRemainingMs = isWhite ? timeWhiteMs : timeBlackMs;
        int incrementMs = isWhite ? incrementWhiteMs : incrementBlackMs;

        int overhead = 50;
        timeRemainingMs -= overhead;
        double optimalThinkTime = Math.min(timeRemainingMs * 0.5, timeRemainingMs * 0.03333 + incrementMs);
        double minThinkTime = Math.min(50, (int) (timeRemainingMs * 0.25));
        double thinkTime = Math.max(optimalThinkTime, minThinkTime);
        return (int) thinkTime;
    }

    /**
     * Terminates the current game, stopping any ongoing search and clearing the board.
     *
     */
    public void gameOver() {
        stopThinking();
        board = null;
    }

    /**
     * Extracts the ponder move from the transposition table after making the best move.
     *
     * @param bestMove the best move to apply
     * @return the ponder move, or null if none exists
     */
    public Move extractPonderMove(Move bestMove) {
        TranspositionTable transpositionTable = searcher.getTranspositionTable();
        board.makeMove(bestMove);
        long zobristKey = board.getGameState().getZobristKey();
        HashEntry entry = transpositionTable.get(zobristKey, 0);
        board.unmakeMove();
        return entry != null ? entry.getMove() : null;
    }

    /**
     * Extracts the principal variation (PV) from the transposition table, up to a maximum depth of 12.
     *
     * @return the list of moves in the principal variation
     */
    public List<Move> extractPrincipalVariation() {
        List<Move> principalVariation = new ArrayList<>();
        TranspositionTable transpositionTable = searcher.getTranspositionTable();
        int moveCount = 0;
        while (moveCount <= 12) {
            long zobristKey = board.getGameState().getZobristKey();
            HashEntry entry = transpositionTable.get(zobristKey, 0);
            if (entry == null || entry.getMove() == null) {
                break;
            }
            principalVariation.add(entry.getMove());
            board.makeMove(entry.getMove());
            moveCount++;
        }
        IntStream.range(0, moveCount).forEach(i -> board.unmakeMove());
        return principalVariation;
    }

    /**
     * Checks if an opening book move is available for the current position.
     *
     * @return true if a book move exists, false otherwise
     */
    private boolean hasBookMove() {
        long key = board.getGameState().getZobristKey();
        int moveCount = board.getMoveHistory().size();
        return config.isOwnBookEnabled() && moveCount < config.getMaxBookMoves() && book.hasBookMove(key);
    }

    /**
     * Retrieves a legal move matching the provided move from the list of generated moves.
     *
     * @param move the move to validate
     * @return the matching legal move
     * @throws IllegalArgumentException if the move is illegal
     */
    private Move getLegalMove(Move move) {
        return moveGenerator.generateMoves(board).stream()
                .filter(move::matches)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Illegal move " + Notation.toNotation(move)));
    }

    /**
     * Performs a synchronous search for the best move, returning immediately if a book move is available.
     *
     * @param thinkTimeMs the time limit for the search in milliseconds
     * @return the search result containing the best move and evaluation
     */
    public SearchResult thinkSync(int thinkTimeMs) {
        if (hasBookMove()) {
            Move bookMove = getLegalMove(book.getBookMove(board.getGameState().getZobristKey()));
            return new SearchResult(0, bookMove, 0, 0, 0, 0);
        }
        stopThinking();
        think = CompletableFuture.supplyAsync(() -> think(thinkTimeMs));
        return think.join();
    }
}