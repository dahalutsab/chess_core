package com.utsab.chess_engine.core.search.moveordering;

import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.Move;
import com.utsab.chess_engine.core.board.Piece;
import com.utsab.chess_engine.core.generation.MagicBitboards;

/**
 * Performs Static Exchange Evaluation (SEE) to estimate material changes after a series of captures on a single square.
 * Used in quiescence search to prune detrimental captures, optimizing search speed in a UCI-compatible chess engine.
 * <p>
 * Uses the bitboard 'swap' algorithm: attackers are found with a single lookup per piece type, and x-ray attackers are
 * revealed by removing pieces from a local occupancy mask. The board itself is never modified, which makes this far
 * cheaper than playing out the capture sequence with make/unmake.
 *
 * @author Utsab Dahal
 * @version 1.1
 * @see <a href="https://www.chessprogramming.org/Static_Exchange_Evaluation">Chess Programming Wiki - Static Exchange Evaluation</a>
 * @see <a href="https://www.chessprogramming.org/SEE_-_The_Swap_Algorithm">Chess Programming Wiki - The Swap Algorithm</a>
 * @since 2025
 */
public class StaticExchangeEvaluator {

    /**
     * Piece values used during the exchange, indexed by {@link Piece#getIndex()}. The king is given a huge value so
     * that capturing with the king onto a defended square is always rejected.
     */
    private static final int[] SEE_VALUES = {100, 320, 330, 500, 900, 20000};

    private static final Piece[] PIECES_BY_VALUE = {Piece.PAWN, Piece.KNIGHT, Piece.BISHOP, Piece.ROOK, Piece.QUEEN, Piece.KING};

    private final int[] gain = new int[32];

    /**
     * Evaluates the material change after a capture sequence initiated by a move.
     *
     * @param board the current board position
     * @param move  the capture move to evaluate
     * @return the net material change (positive for gain, negative for loss)
     */
    public int evaluate(Board board, Move move) {
        int startSquare = move.getStartSquare();
        int square = move.getEndSquare();
        boolean isWhite = board.isWhiteToMove();

        Piece capturedPiece = move.isEnPassant() ? Piece.PAWN : board.pieceAt(square);
        Piece movingPiece = move.isPromotion() ? move.getPromotionPieceType() : board.pieceAt(startSquare);

        long occupied = board.getOccupied() ^ (1L << startSquare);
        if (move.isEnPassant()) {
            occupied ^= 1L << (isWhite ? square - 8 : square + 8);
        }
        return swap(board, square, occupied, valueOf(capturedPiece), valueOf(movingPiece), !isWhite);
    }

    /**
     * Evaluates the material balance of a move that has already been made on the board, from the perspective of the
     * side that made it. Used to check whether a checking move simply hangs the checking piece.
     *
     * @param board the board, with the move already applied
     * @param move  the move that was just made
     * @return the net material change for the side that made the move
     */
    public int evaluateAfterMove(Board board, Move move) {
        int square = move.getEndSquare();
        Piece capturedPiece = board.getGameState().getCapturedPiece();
        Piece movedPiece = board.pieceAt(square);
        return swap(board, square, board.getOccupied(), valueOf(capturedPiece), valueOf(movedPiece), board.isWhiteToMove());
    }

    /**
     * Plays out the capture sequence on the given square, always recapturing with the least valuable attacker, and
     * returns the result assuming either side may stop capturing whenever continuing would lose material.
     *
     * @param square             the square being fought over
     * @param occupied           occupancy after the initial capture
     * @param initialGain        value of the piece taken by the initial capture
     * @param pieceOnSquareValue value of the piece now standing on the square
     * @param isWhite            the side to make the next capture
     */
    private int swap(Board board, int square, long occupied, int initialGain, int pieceOnSquareValue, boolean isWhite) {
        long diagonalSliders = board.getBishops(true) | board.getBishops(false) | board.getQueens(true) | board.getQueens(false);
        long orthogonalSliders = board.getRooks(true) | board.getRooks(false) | board.getQueens(true) | board.getQueens(false);
        long attackers = attackersTo(board, square, occupied, diagonalSliders, orthogonalSliders) & occupied;

        int depth = 0;
        gain[0] = initialGain;

        while (true) {
            long sideAttackers = attackers & board.getPieces(isWhite);
            if (sideAttackers == 0) break;

            // Find the least valuable attacker for the side to capture
            Piece attacker = null;
            long attackerBit = 0;
            for (Piece piece : PIECES_BY_VALUE) {
                long candidates = sideAttackers & pieces(board, piece, isWhite);
                if (candidates != 0) {
                    attacker = piece;
                    attackerBit = candidates & -candidates;
                    break;
                }
            }
            if (attacker == null) break;

            // The king may only recapture if the square is no longer defended
            if (attacker == Piece.KING && (attackers & ~attackerBit & board.getPieces(!isWhite)) != 0) break;

            depth++;
            gain[depth] = pieceOnSquareValue - gain[depth - 1];
            pieceOnSquareValue = SEE_VALUES[attacker.getIndex()];

            // Remove the attacker and reveal any x-ray attackers behind it
            occupied ^= attackerBit;
            attackers |= (MagicBitboards.bishopAttacks(square, occupied) & diagonalSliders)
                    | (MagicBitboards.rookAttacks(square, occupied) & orthogonalSliders);
            attackers &= occupied;
            isWhite = !isWhite;

            if (depth == gain.length - 1) break;
        }

        while (depth > 0) {
            gain[depth - 1] = -Math.max(-gain[depth - 1], gain[depth]);
            depth--;
        }
        return gain[0];
    }

    private long attackersTo(Board board, int square, long occupied, long diagonalSliders, long orthogonalSliders) {
        long squareMask = 1L << square;
        return (MagicBitboards.pawnAttacks(squareMask, false) & board.getPawns(true))
                | (MagicBitboards.pawnAttacks(squareMask, true) & board.getPawns(false))
                | (MagicBitboards.knightAttacks(square) & (board.getKnights(true) | board.getKnights(false)))
                | (MagicBitboards.kingAttacks(square) & (board.getKing(true) | board.getKing(false)))
                | (MagicBitboards.bishopAttacks(square, occupied) & diagonalSliders)
                | (MagicBitboards.rookAttacks(square, occupied) & orthogonalSliders);
    }

    private long pieces(Board board, Piece piece, boolean isWhite) {
        return switch (piece) {
            case PAWN -> board.getPawns(isWhite);
            case KNIGHT -> board.getKnights(isWhite);
            case BISHOP -> board.getBishops(isWhite);
            case ROOK -> board.getRooks(isWhite);
            case QUEEN -> board.getQueens(isWhite);
            case KING -> board.getKing(isWhite);
        };
    }

    private static int valueOf(Piece piece) {
        return piece != null ? SEE_VALUES[piece.getIndex()] : 0;
    }
}
