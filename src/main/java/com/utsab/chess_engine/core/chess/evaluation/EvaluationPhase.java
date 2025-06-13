package com.utsab.chess_engine.core.chess.evaluation;

import com.utsab.chess_engine.core.chess.board.Piece;
import com.utsab.chess_engine.core.chess.engine.EngineConfig;


/**
 * Utility class for calculating the game phase and performing tapered evaluation in a chess engine.
 * <p>
 * The game phase is a value between 0 and 1, where 1 represents the opening/early middlegame (all major and minor pieces present),
 * and 0 represents the endgame (only kings and pawns remain). This value is used to interpolate between middlegame and endgame
 * evaluation scores, reflecting the changing value of pieces as the game progresses.
 * </p>
 *
 * <p>
 * The calculation is based on the number and type of pieces remaining on the board, using configurable phase values for each piece type.
 * </p>
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Tapered_Eval">Chess Programming Wiki: Tapered Eval</a>
 */
public class EvaluationPhase {

    /**
     * Calculates the normalized game phase value for both sides, based on the remaining material.
     * <p>
     * The phase value is computed as the sum of the phase values of all knights, bishops, rooks, and queens
     * for both White and Black, divided by the total possible phase value (from {@link EngineConfig#getTotalPhase()}).
     * </p>
     *
     * @param whiteMaterial the material counts for White
     * @param blackMaterial the material counts for Black
     * @param config        the engine configuration containing phase values for each piece type
     * @return the normalized phase value in the range [0, 1], where 1 is opening and 0 is endgame
     */
    public static float calculatePhase(Material whiteMaterial, Material blackMaterial, EngineConfig config) {
        int knightPhase = config.getPiecePhases()[Piece.KNIGHT.getIndex()];
        int bishopPhase = config.getPiecePhases()[Piece.BISHOP.getIndex()];
        int rookPhase = config.getPiecePhases()[Piece.ROOK.getIndex()];
        int queenPhase = config.getPiecePhases()[Piece.QUEEN.getIndex()];
        int currentMaterial =
            (whiteMaterial.knights() * knightPhase) +
            (blackMaterial.knights() * knightPhase) +
            (whiteMaterial.bishops() * bishopPhase) +
            (blackMaterial.bishops() * bishopPhase) +
            (whiteMaterial.rooks() * rookPhase) +
            (blackMaterial.rooks() * rookPhase) +
            (whiteMaterial.queens() * queenPhase) +
            (blackMaterial.queens() * queenPhase);
        return currentMaterial / config.getTotalPhase();
    }

    /**
     * Calculates the normalized game phase value for a single side, based on the remaining material.
     * <p>
     * The phase value is computed as the sum of the phase values of all knights, bishops, rooks, and queens
     * for the given side, divided by the total possible phase value (from {@link EngineConfig#getTotalPhase()}).
     * </p>
     *
     * @param material the material counts for one side
     * @param config   the engine configuration containing phase values for each piece type
     * @return the normalized phase value in the range [0, 1], where 1 is opening and 0 is endgame
     */
    public static float calculatePhase(Material material, EngineConfig config) {
        int knightPhase = config.getPiecePhases()[Piece.KNIGHT.getIndex()];
        int bishopPhase = config.getPiecePhases()[Piece.BISHOP.getIndex()];
        int rookPhase = config.getPiecePhases()[Piece.ROOK.getIndex()];
        int queenPhase = config.getPiecePhases()[Piece.QUEEN.getIndex()];
        int currentMaterial =
                (material.knights() * knightPhase) +
                (material.bishops() * bishopPhase) +
                (material.rooks() * rookPhase) +
                (material.queens() * queenPhase);
        return currentMaterial / config.getTotalPhase();
    }

    /**
     * Interpolates between middlegame and endgame evaluation scores using the given phase value.
     * <p>
     * The returned value is a weighted sum: {@code phase * middleGameScore + (1 - phase) * endgameScore}.
     * </p>
     *
     * @param middleGameScore the evaluation score for the middlegame
     * @param endgameScore    the evaluation score for the endgame
     * @param phase           the normalized phase value in the range [0, 1]
     * @return the tapered evaluation score as an integer
     */
    public static int interpolateEvaluation(int middleGameScore, int endgameScore, float phase) {
        return (int) (phase * middleGameScore) + (int) ((1 - phase) * endgameScore);
    }

}
