package com.utsab.chess_engine.core.evaluation;

import com.utsab.chess_engine.core.board.BitOperations;
import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.Piece;

/**
 * Stores how much material one side has.
 */
public record Material(int pawns,
                       int knights,
                       int bishops,
                       int rooks,
                       int queens) {

    public static Material fromBoard(Board board, boolean isWhite) {
        int pawns = BitOperations.countBits(board.getPawns(isWhite));
        int knights = BitOperations.countBits(board.getKnights(isWhite));
        int bishops = BitOperations.countBits(board.getBishops(isWhite));
        int rooks = BitOperations.countBits(board.getRooks(isWhite));
        int queens = BitOperations.countBits(board.getQueens(isWhite));
        return new Material(pawns, knights, bishops, rooks, queens);
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

}
