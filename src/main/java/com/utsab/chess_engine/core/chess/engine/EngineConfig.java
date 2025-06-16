package com.utsab.chess_engine.core.chess.engine;

import com.utsab.chess_engine.core.chess.board.Piece;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Holds configuration settings for the chess engine, including thread counts, transposition table sizes, search parameters,
 * and evaluation tables. Provides methods to initialize derived configuration values.
 *
 * @author Utsab Dahal
 * version 1.0
 * @since 2025
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Configuration
public class EngineConfig {

    @Bean
    public Engine engine() {
        return EngineInitializer.loadEngine();
    }

    /**
     * The minimum number of threads the engine can use for parallel search.
     */
    int minThreadCount;

    /**
     * The maximum number of threads the engine can use for parallel search.
     */
    int maxThreadCount;

    /**
     * The default number of threads to use for parallel search if not specified.
     */
    int defaultThreadCount;

    /**
     * The minimum size of the transposition table in megabytes.
     */
    int minHashSizeMb;

    /**
     * The maximum size of the transposition table in megabytes.
     */
    int maxHashSizeMb;

    /**
     * The default size of the transposition table in megabytes if not specified.
     */
    int defaultHashSizeMb;

    /**
     * Indicates whether the engine's opening book is enabled.
     */
    boolean ownBookEnabled;

    /**
     * The maximum number of moveEntities to consider from the opening book.
     */
    int maxBookMoves;

    /**
     * Indicates whether pondering (thinking during the opponent's turn) is enabled.
     */
    boolean ponderEnabled;

    /**
     * The maximum length of the principal variation to store or report.
     */
    int principalVariationLength;

    /**
     * Indicates whether the engine is currently in pondering mode.
     */
    boolean pondering = false;

    /**
     * Indicates whether the current search should be cancelled.
     */
    boolean searchCancelled = false;

    /**
     * The maximum search depth for the engine's search algorithm.
     */
    int maxDepth;

    /**
     * The maximum number of possible moveEntities in a position, used for array sizing.
     */
    int maxPossibleMoves = 250;

    /**
     * The aspiration window margin for alpha-beta search optimization.
     */
    int aspMargin;

    /**
     * The margin for aspiration window failure, triggering a wider search.
     */
    int aspFailMargin;

    /**
     * The minimum depth for null move pruning in the search algorithm.
     */
    int nmpDepth;

    /**
     * The minimum depth for futility pruning in the search algorithm.
     */
    int fpDepth;

    /**
     * The minimum depth for reverse futility pruning in the search algorithm.
     */
    int rfpDepth;

    /**
     * The minimum depth for late move reductions in the search algorithm.
     */
    int lmrDepth;

    /**
     * The base value for the late move reduction formula.
     */
    float lmrBase;

    /**
     * The divisor used in the late move reduction formula to scale reductions.
     */
    float lmrDivisor;

    /**
     * The minimum number of moveEntities searched before applying late move reductions.
     */
    int lmrMinSearchedMoves;

    /**
     * The minimum depth for late move pruning in the search algorithm.
     */
    int lmpDepth;

    /**
     * The multiplier used to scale late move pruning thresholds.
     */
    int lmpMultiplier;

    /**
     * The minimum depth for internal iterative reduction in the search algorithm.
     */
    int iirDepth;

    /**
     * The margin for null move pruning evaluation checks.
     */
    int nmpMargin;

    /**
     * The margin for delta pruning in the search algorithm.
     */
    int dpMargin;

    /**
     * The table of late move reduction values, indexed by depth and moveEntities searched.
     */
    int[][] lmrReductions;

    /**
     * The futility pruning margins for each depth.
     */
    int[] fpMargin;

    /**
     * The reverse futility pruning margins for each depth.
     */
    int[] rfpMargin;

    /**
     * The phase values for each piece type, used in game phase calculations.
     */
    int[] piecePhases;

    /**
     * The total phase value for evaluation, derived from piece phase contributions.
     */
    float totalPhase;

    /**
     * The piece values for evaluation, indexed by piece type and game phase.
     */
    int[][] pieceValues;

    /**
     * The piece-square tables for the middlegame, used in position evaluation.
     */
    int[][] middlegameTables;

    /**
     * The piece-square tables for the endgame, used in position evaluation.
     */
    int[][] endgameTables;

    /**
     * The mobility bonuses for pieces in the middlegame, indexed by piece type and mobility.
     */
    int[][] middlegameMobilityBonus;

    /**
     * The mobility bonuses for pieces in the endgame, indexed by piece type and mobility.
     */
    int[][] endgameMobilityBonus;

    /**
     * The penalties for isolated pawns, indexed by file and side.
     */
    int[][] isolatedPawnPenalty;

    /**
     * The penalties for doubled pawns, indexed by file and side.
     */
    int[][] doubledPawnPenalty;

    /**
     * The bonuses for passed pawns, indexed by rank and side.
     */
    int[][] passedPawnBonus;

    /**
     * The bonus for protected passed pawns.
     */
    int protectedPassedPawnBonus;

    /**
     * The bonus for having a bishop pair in the evaluation.
     */
    int bishopPairBonus;

    /**
     * The bonuses for rooks on open files, indexed by side.
     */
    int[] rookOpenFileBonus;

    /**
     * The bonuses for rooks on semi-open files, indexed by side.
     */
    int[] rookSemiOpenFileBonus;

    /**
     * The penalties for king safety based on pawn shield weaknesses.
     */
    int[] kingPawnShieldPenalty;

    /**
     * The penalty for the king on a semi-open file.
     */
    int kingSemiOpenFilePenalty;

    /**
     * The penalty for the king on a file adjacent to a semi-open file.
     */
    int kingSemiOpenAdjacentFilePenalty;

    /**
     * The penalty for the king on an open file.
     */
    int kingOpenFilePenalty;

    /**
     * The penalty for the king on a file adjacent to an open file.
     */
    int kingOpenAdjacentFilePenalty;

    /**
     * The penalty for losing castling rights in the evaluation.
     */
    int kingLostCastlingRightsPenalty;

    /**
     * The multiplier for king safety based on Manhattan distance to threats.
     */
    int kingManhattanDistanceMultiplier;

    /**
     * The multiplier for king safety based on Chebyshev distance to threats.
     */
    int kingChebyshevDistanceMultiplier;

    /**
     * The multiplier for king distance to the center in Manhattan distance.
     */
    int kingCenterManhattanDistanceMultiplier;

    /**
     * The bonus for the side to move in the evaluation.
     */
    int tempoBonus;

    /**
     * Initializes derived configuration values, such as late move reduction tables and total phase for evaluation.
     */
    public void postInitialise() {
        calculateLmrReductions();
        calculateTotalPhase();
    }

    /**
     * Calculates the late move reduction (LMR) table based on depth and moveEntities searched, using a logarithmic formula.
     */
    private void calculateLmrReductions() {
        // Credit to Lynx (https://github.com/lynx-chess/Lynx) for this formula for determining the optimal late move reduction depth
        lmrReductions = new int[maxDepth][];
        for (int depth = 1; depth < maxDepth; ++depth) {
            lmrReductions[depth] = new int[maxPossibleMoves];
            for (int movesSearched = 1; movesSearched < maxPossibleMoves; ++movesSearched) {
                lmrReductions[depth][movesSearched] = (int) Math.round(lmrBase + (Math.log(movesSearched) * Math.log(depth) / lmrDivisor));
            }
        }
    }

    /**
     * Calculates the total phase value for evaluation based on piece phase contributions.
     */
    private void calculateTotalPhase() {
        int knightPhase = piecePhases[Piece.KNIGHT.getIndex()];
        int bishopPhase = piecePhases[Piece.BISHOP.getIndex()];
        int rookPhase = piecePhases[Piece.ROOK.getIndex()];
        int queenPhase = piecePhases[Piece.QUEEN.getIndex()];
        totalPhase = (knightPhase * 4) + (bishopPhase * 4) + (rookPhase * 4) + (queenPhase * 2);
    }
}