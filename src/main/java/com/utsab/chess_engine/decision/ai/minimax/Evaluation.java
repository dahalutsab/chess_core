package com.utsab.chess_engine.decision.ai.minimax;

import com.utsab.chess_engine.decision.board.Bitboard;
import com.utsab.chess_engine.decision.board.Bitboard.MoveInfo;
import com.utsab.chess_engine.decision.board.MoveGenerator;
import lombok.extern.slf4j.Slf4j;


/**
 * Evaluates a chess position based on material and positional factors using piece-square tables.
 * Scores are in centipawns (1 pawn = 100).
 */
@Slf4j
public class Evaluation {
    private Evaluation() {} // Prevent instantiation

    // Material values in centipawns
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    private static final int KING_VALUE = 20000; // Used only for checkmate detection, not evaluation

    // Piece-square tables (64 entries each, for squares a1 to h8)
    // Values are tuned for opening/middlegame; for simplicity, we use one set (could split for endgame)

    /** Pawn table: Encourages advancement and center control */
    private static final int[] PAWN_TABLE = {
            0,  0,  0,  0,  0,  0,  0,  0,  // Rank 1 (no pawns here naturally)
            50, 50, 50, 50, 50, 50, 50, 50,  // Rank 2 (base value for advancement)
            10, 10, 20, 30, 30, 20, 10, 10,  // Rank 3
            5,  5, 10, 25, 25, 10,  5,  5,  // Rank 4
            0,  0,  0, 20, 20,  0,  0,  0,  // Rank 5
            5, -5,-10,  0,  0,-10, -5,  5,  // Rank 6
            5, 10, 10,-20,-20, 10, 10,  5,  // Rank 7
            0,  0,  0,  0,  0,  0,  0,  0   // Rank 8 (promotion handled separately)
    };

    /** Knight table: Favors center squares (e.g., d4, e4, d5, e5) */
    private static final int[] KNIGHT_TABLE = {
            -50,-40,-30,-30,-30,-30,-40,-50,  // Rank 1
            -40,-20,  0,  0,  0,  0,-20,-40,  // Rank 2
            -30,  0, 10, 15, 15, 10,  0,-30,  // Rank 3
            -30,  5, 15, 20, 20, 15,  5,-30,  // Rank 4
            -30,  0, 15, 20, 20, 15,  0,-30,  // Rank 5
            -30,  5, 10, 15, 15, 10,  5,-30,  // Rank 6
            -40,-20,  0,  5,  5,  0,-20,-40,  // Rank 7
            -50,-40,-30,-30,-30,-30,-40,-50   // Rank 8
    };

    /** Bishop table: Encourages diagonals and center control */
    private static final int[] BISHOP_TABLE = {
            -20,-10,-10,-10,-10,-10,-10,-20,  // Rank 1
            -10,  0,  0,  0,  0,  0,  0,-10,  // Rank 2
            -10,  0,  5, 10, 10,  5,  0,-10,  // Rank 3
            -10,  5,  5, 10, 10,  5,  5,-10,  // Rank 4
            -10,  0, 10, 10, 10, 10,  0,-10,  // Rank 5
            -10, 10, 10, 10, 10, 10, 10,-10,  // Rank 6
            -10,  5,  0,  0,  0,  0,  5,-10,  // Rank 7
            -20,-10,-10,-10,-10,-10,-10,-20   // Rank 8
    };

    /** Rook table: Prefers 7th rank and center files */
    private static final int[] ROOK_TABLE = {
            0,  0,  0,  0,  0,  0,  0,  0,  // Rank 1
            5, 10, 10, 10, 10, 10, 10,  5,  // Rank 2
            -5,  0,  0,  0,  0,  0,  0, -5,  // Rank 3
            -5,  0,  0,  0,  0,  0,  0, -5,  // Rank 4
            -5,  0,  0,  0,  0,  0,  0, -5,  // Rank 5
            -5,  0,  0,  0,  0,  0,  0, -5,  // Rank 6
            5, 10, 10, 10, 10, 10, 10,  5,  // Rank 7
            0,  0,  0,  5,  5,  0,  0,  0   // Rank 8
    };

    /** Queen table: Encourages center control and activity */
    private static final int[] QUEEN_TABLE = {
            -20,-10,-10, -5, -5,-10,-10,-20,  // Rank 1
            -10,  0,  0,  0,  0,  0,  0,-10,  // Rank 2
            -10,  0,  5,  5,  5,  5,  0,-10,  // Rank 3
            -5,  0,  5, 10, 10,  5,  0, -5,  // Rank 4
             0,  0, 10, 10, 10, 10,  0, -5,  // Rank 5
            -10,  5,  5, 10, 10,  5,  0,-10,  // Rank 6
            -10,  0,  5,  0,  0,  0,  0,-10,  // Rank 7
            -20,-10,-10, -5, -5,-10,-10,-20   // Rank 8
    };

    /** King table: Prefers corners in opening/middlegame, center in endgame (simplified here) */
    private static final int[] KING_TABLE = {
            -30,-40,-40,-50,-50,-40,-40,-30,  // Rank 1
            -30,-40,-40,-50,-50,-40,-40,-30,  // Rank 2
            -30,-40,-40,-50,-50,-40,-40,-30,  // Rank 3
            -30,-40,-40,-50,-50,-40,-40,-30,  // Rank 4
            -20,-30,-30,-40,-40,-30,-30,-20,  // Rank 5
            -10,-20,-20,-20,-20,-20,-20,-10,  // Rank 6
            10, 10,  0,  0,  0,  0, 10, 10,  // Rank 7
            20, 30, 10,  0,  0, 10, 30, 20   // Rank 8
    };

    /**
     * Evaluates the given board position.
     *
     * @param board the Bitboard to evaluate
     * @return the evaluation score in centipawns (positive favors White, negative favors Black)
     */
    public static int evaluate(Bitboard board) {
        int score = 0;

        // Material and positional evaluation
        score += materialAndPosition(board, true);  // White
        score -= materialAndPosition(board, false); // Black

        // Mobility bonus
        score += mobility(board);

        return score;
    }

    private static int materialAndPosition(Bitboard board, boolean white) {
        int score = 0;
        int base = white ? 0 : 6;
        int[] materialValues = {PAWN_VALUE, KNIGHT_VALUE, BISHOP_VALUE, ROOK_VALUE, QUEEN_VALUE, KING_VALUE};
        int[][] pieceTables = {PAWN_TABLE, KNIGHT_TABLE, BISHOP_TABLE, ROOK_TABLE, QUEEN_TABLE, KING_TABLE};

        for (int i = 0; i < 6; i++) {
            long pieces = board.getPieceBitboard(base + i);
            score += Long.bitCount(pieces) * materialValues[i];
            while (pieces != 0) {
                int square = Long.numberOfTrailingZeros(pieces);
                int tableIndex = white ? square : (63 - square);
                score += pieceTables[i][tableIndex];
                pieces &= pieces - 1;
            }
        }
        return score;
    }

    private static int mobility(Bitboard board) {
        MoveGenerator mg = new MoveGenerator(board);
        int whiteMobility = mg.generateMoves().size();
        board.setWhiteToMove(!board.isWhiteToMove());
        int blackMobility = mg.generateMoves().size();
        board.setWhiteToMove(!board.isWhiteToMove());
        return (whiteMobility - blackMobility) * 10;
    }

    public static boolean isCapture(MoveInfo moveInfo) {
        return moveInfo.capturedPieceIndex() != -1;
    }

    public static int captureValue(MoveInfo moveInfo) {
        if (!isCapture(moveInfo)) return 0;
        return switch (moveInfo.capturedPieceIndex() % 6) {
            case 0 -> PAWN_VALUE;
            case 1 -> KNIGHT_VALUE;
            case 2 -> BISHOP_VALUE;
            case 3 -> ROOK_VALUE;
            case 4 -> QUEEN_VALUE;
            case 5 -> KING_VALUE;
            default -> 0;
        };
    }
}