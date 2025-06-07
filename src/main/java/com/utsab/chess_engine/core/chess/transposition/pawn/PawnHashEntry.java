package com.utsab.chess_engine.core.chess.transposition.pawn;

public record PawnHashEntry(long key,
                            PawnScore whiteScore,
                            PawnScore blackScore) {

    public record PawnScore(int pawnPlacementMgScore,
                                   int pawnPlacementEgScore,
                                   int pawnStructureMgScore,
                                   int pawnStructureEgScore) {

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
