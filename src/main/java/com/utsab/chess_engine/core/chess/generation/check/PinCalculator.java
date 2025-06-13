package com.utsab.chess_engine.core.chess.generation.check;

import com.utsab.chess_engine.core.chess.board.BitwiseOperations;
import com.utsab.chess_engine.core.chess.board.Board;
import com.utsab.chess_engine.core.chess.generation.MagicBitboards;

public class PinCalculator {

    private final RayCalculator rayCalculator = new RayCalculator();

    private long pinMask;
    private long[] pinRayMasks;

    public record PinData(long pinMask, long[] pinRayMasks) {}

    /**
     * Calculates the pin mask and pin ray masks for the given board position.
     *
     * @param board The game board.
     * @param isWhite Whether the current player is white.
     * @return The pin data containing the pin mask and pin ray masks.
     */
    public PinData calculatePinMask(Board board, boolean isWhite) {
        pinMask = 0L;
        pinRayMasks = new long[64];

        int kingSquare = BitwiseOperations.getNextBit(board.getKing(isWhite));
        long friendlies = board.getPieces(isWhite);
        long opponents = board.getPieces(!isWhite);

        // Calculate possible orthogonal (queen or rook) pins
        long opponentOrthogonalSliders = board.getRooks(!isWhite) | board.getQueens(!isWhite);
        if (opponentOrthogonalSliders != 0) {
            long possibleOrthogonalPinners = MagicBitboards.rookAttacks(kingSquare, 0) & opponentOrthogonalSliders;
            calculatePins(kingSquare, friendlies, opponents, possibleOrthogonalPinners);
        }

        // Calculate possible diagonal (queen or bishop) pins
        long opponentDiagonalSliders = board.getBishops(!isWhite) | board.getQueens(!isWhite);
        if (opponentDiagonalSliders != 0) {
            long possibleDiagonalPinners = MagicBitboards.bishopAttacks(kingSquare, 0) & opponentDiagonalSliders;
            calculatePins(kingSquare, friendlies, opponents, possibleDiagonalPinners);
        }

        return new PinData(pinMask, pinRayMasks);
    }

    /**
     * Calculates pins between the king and potential pinners.
     *
     * @param kingSquare The square of the king.
     * @param friendlies The bitboard of friendly pieces.
     * @param opponents The bitboard of opponent pieces.
     * @param possiblePinners The bitboard of possible pinners.
     */
    private void calculatePins(int kingSquare, long friendlies, long opponents, long possiblePinners) {
        while (possiblePinners != 0) {
            int possiblePinner = BitwiseOperations.getNextBit(possiblePinners);
            long ray = rayCalculator.rayBetween(kingSquare, possiblePinner);

            // Skip if there are opponents between the king and the possible pinner
            if ((ray & opponents) != 0) {
                possiblePinners = BitwiseOperations.popBit(possiblePinners);
                continue;
            }

            long friendliesBetween = ray & friendlies;
            // If there is exactly one friendly piece between the king and the pinner, it's pinned
            if (BitwiseOperations.countBits(friendliesBetween) == 1) {
                int friendlySquare = BitwiseOperations.getNextBit(friendliesBetween);
                pinMask |= friendliesBetween;
                pinRayMasks[friendlySquare] = ray | (1L << possiblePinner);
            }

            possiblePinners = BitwiseOperations.popBit(possiblePinners);
        }
    }
}
