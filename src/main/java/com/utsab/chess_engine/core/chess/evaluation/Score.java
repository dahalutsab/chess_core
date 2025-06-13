package com.utsab.chess_engine.core.chess.evaluation;

import com.utsab.chess_engine.core.chess.board.Piece;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Data
@NoArgsConstructor
public class Score {

    public static final int MATE_SCORE = 1000000;
    public static final int DRAW_SCORE = 0;
    public static final int[] SIMPLE_PIECE_VALUES = Arrays.stream(Piece.values()).mapToInt(Piece::getValue).toArray();

    public static boolean isMateScore(int eval) {
        return Math.abs(eval) >= Score.MATE_SCORE - 100;
    }

    int whiteMaterialMgScore;
    int whiteMaterialEgScore;
    int blackMaterialMgScore;
    int blackMaterialEgScore;

    int whitePiecePlacementScore;
    int blackPiecePlacementScore;

    int whiteMobilityMgScore;
    int whiteMobilityEgScore;
    int blackMobilityMgScore;
    int blackMobilityEgScore;

    int whitePawnStructureMgScore;
    int whitePawnStructureEgScore;
    int blackPawnStructureMgScore;
    int blackPawnStructureEgScore;

    int whiteKingSafetyScore;
    int blackKingSafetyScore;

    int whiteRookMgScore;
    int whiteRookEgScore;
    int blackRookMgScore;
    int blackRookEgScore;

    int whiteMopUpScore;
    int blackMopUpScore;

    int whiteTempoBonus;
    int blackTempoBonus;

    float phase;

    public void addMaterialScore(int middlegameScore, int endgameScore, boolean isWhite) {
        if (isWhite) {
            whiteMaterialMgScore += middlegameScore;
            whiteMaterialEgScore += endgameScore;
        } else {
            blackMaterialMgScore += middlegameScore;
            blackMaterialEgScore += endgameScore;
        }
    }

    public void addPiecePlacementScore(int middlegameScore, int endgameScore, boolean isWhite) {
        if (isWhite) {
            whitePiecePlacementScore += EvaluationPhase.interpolateEvaluation(middlegameScore, endgameScore, phase);
        } else {
            blackPiecePlacementScore += EvaluationPhase.interpolateEvaluation(middlegameScore, endgameScore, phase);
        }
    }

    public void addMobilityScore(int middlegameScore, int endgameScore, boolean isWhite) {
        if (isWhite) {
            whiteMobilityMgScore += middlegameScore;
            whiteMobilityEgScore += endgameScore;
        } else {
            blackMobilityMgScore += middlegameScore;
            blackMobilityEgScore += endgameScore;
        }
    }

    public void addPawnStructureScore(int middlegameScore, int endgameScore, boolean isWhite) {
        if (isWhite) {
            whitePawnStructureMgScore += middlegameScore;
            whitePawnStructureEgScore += endgameScore;
        } else {
            blackPawnStructureMgScore += middlegameScore;
            blackPawnStructureEgScore += endgameScore;
        }
    }

    public void addRookScore(int middlegameScore, int endgameScore, boolean isWhite) {
        if (isWhite) {
            whiteRookMgScore += middlegameScore;
            whiteRookEgScore += endgameScore;
        } else {
            blackRookMgScore += middlegameScore;
            blackRookEgScore += endgameScore;
        }
    }

    public void setKingSafetyScore(int score, boolean isWhite) {
        if (isWhite) {
            whiteKingSafetyScore = score;
        } else {
            blackKingSafetyScore = score;
        }
    }

    public void setMopUpScore(int score, boolean isWhite) {
        if (isWhite) {
            whiteMopUpScore = score;
        } else {
            blackMopUpScore = score;
        }
    }

    public int sum(boolean isWhite) {

        int whiteMaterialScore = EvaluationPhase.interpolateEvaluation(whiteMaterialMgScore, whiteMaterialEgScore, phase);
        int blackMaterialScore = EvaluationPhase.interpolateEvaluation(blackMaterialMgScore, blackMaterialEgScore, phase);

        int whiteMobilityScore = EvaluationPhase.interpolateEvaluation(whiteMobilityMgScore, whiteMobilityEgScore, phase);
        int blackMobilityScore = EvaluationPhase.interpolateEvaluation(blackMobilityMgScore, blackMobilityEgScore, phase);

        int whitePawnStructureScore = EvaluationPhase.interpolateEvaluation(whitePawnStructureMgScore, whitePawnStructureEgScore, phase);
        int blackPawnStructureScore = EvaluationPhase.interpolateEvaluation(blackPawnStructureMgScore, blackPawnStructureEgScore, phase);

        int whiteRookScore = EvaluationPhase.interpolateEvaluation(whiteRookMgScore, whiteRookEgScore, phase);
        int blackRookScore = EvaluationPhase.interpolateEvaluation(blackRookMgScore, blackRookEgScore, phase);

        int whiteScore = whiteMaterialScore + whitePiecePlacementScore + whiteMobilityScore + whitePawnStructureScore +
                whiteKingSafetyScore + whiteRookScore + whiteMopUpScore + whiteTempoBonus;

        int blackScore = blackMaterialScore + blackPiecePlacementScore + blackMobilityScore + blackPawnStructureScore +
                blackKingSafetyScore + blackRookScore + blackMopUpScore + blackTempoBonus;

        int modifier = isWhite ? 1 : -1;
        return modifier * (whiteScore - blackScore);

    }

}
