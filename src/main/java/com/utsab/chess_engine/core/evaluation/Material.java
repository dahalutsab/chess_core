package com.utsab.chess_engine.core.evaluation;

import com.utsab.chess_engine.core.board.BitwiseOperations;
import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.Piece;
import com.utsab.chess_engine.core.engine.EngineConfig;

/**
 * Stores how much material one side has.
 */
public record Material(int pawns,
                       int knights,
                       int bishops,
                       int rooks,
                       int queens) {

    public static Material fromBoard(Board board, boolean isWhite) {
        int pawns = BitwiseOperations.countBits(board.getPawns(isWhite));
        int knights = BitwiseOperations.countBits(board.getKnights(isWhite));
        int bishops = BitwiseOperations.countBits(board.getBishops(isWhite));
        int rooks = BitwiseOperations.countBits(board.getRooks(isWhite));
        int queens = BitwiseOperations.countBits(board.getQueens(isWhite));
        return new Material(pawns, knights, bishops, rooks, queens);
    }

    public int sum(EngineConfig config, float phase) {
        int middlegameScore = sum(config.getPieceValues()[0], config.getBishopPairBonus());
        int endgameScore = sum(config.getPieceValues()[1], config.getBishopPairBonus());
        return EvaluationPhase.interpolateEvaluation(middlegameScore, endgameScore, phase);
    }

    /**
     * Based on a provided piece-value array, counts the material score in centipawns.
     */
    public int sum(int[] pieceValues, int bishopPairBonus) {
        return (pawns * pieceValues[Piece.PAWN.getIndex()]) +
                (knights * pieceValues[Piece.KNIGHT.getIndex()]) +
                (bishops * pieceValues[Piece.BISHOP.getIndex()]) +
                (rooks * pieceValues[Piece.ROOK.getIndex()]) +
                (queens * pieceValues[Piece.QUEEN.getIndex()]) +
                (bishops == 2 ? bishopPairBonus : 0);
    }

    public boolean hasPiecesRemaining() {
        return knights > 0 || bishops > 0 || rooks > 0 || queens > 0;
    }

}
