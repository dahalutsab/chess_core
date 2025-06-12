package com.utsab.chess_engine.core.chess.transposition.pawn;

/**
 * Represents an entry in the pawn hash table, storing a pawn Zobrist key and evaluation scores for both sides.
 * Each side's scores include midgame and endgame values for pawn placement and structure, used to optimize
 * pawn evaluation in a UCI-compatible chess engine.
 *
 * @param key the 64-bit pawn Zobrist key
 * @param whiteScore the scores for White pawns
 * @param blackScore the scores for Black pawns
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 */
public record PawnHashEntry(long key, PawnScore whiteScore, PawnScore blackScore) {

    /**
     * Represents pawn evaluation scores for one side, including midgame and endgame values for pawn placement
     * and structure.
     *
     * @param pawnPlacementMgScore midgame score for pawn placement
     * @param pawnPlacementEgScore endgame score for pawn placement
     * @param pawnStructureMgScore midgame score for pawn structure
     * @param pawnStructureEgScore endgame score for pawn structure
     */
    public record PawnScore(int pawnPlacementMgScore, int pawnPlacementEgScore,
                            int pawnStructureMgScore, int pawnStructureEgScore) {

        /**
         * Compares this PawnScore with another object for equality.
         *
         * @param other the object to compare
         * @return true if the other object is a PawnScore with identical values, false otherwise
         */
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof PawnScore(
                    int placementMgScore, int placementEgScore, int structureMgScore, int structureEgScore
            ))) {
                return false;
            }
            return this.pawnPlacementMgScore == placementMgScore
                    && this.pawnPlacementEgScore == placementEgScore
                    && this.pawnStructureMgScore == structureMgScore
                    && this.pawnStructureEgScore == structureEgScore;
        }
    }
}