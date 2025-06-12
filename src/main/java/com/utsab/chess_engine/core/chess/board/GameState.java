package com.utsab.chess_engine.core.chess.board;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates metadata for a chess position, including Zobrist keys, castling rights, en passant file,
 * fifty-move counter, and the last captured piece. Used by the {@link Board} class to track game state
 * history and support move unmaking in a UCI-compatible chess engine.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameState {

    /**
     * Zobrist key representing the entire board position.
     */
    private long zobristKey = 0L;

    /**
     * Zobrist key for pawn structure only.
     */
    private long pawnKey = 0L;

    /**
     * The piece captured in the last move, or null if none.
     */
    private Piece capturedPiece;

    /**
     * The file index (-1 or 0-7) for the en passant target square, or -1 if none.
     */
    private int enPassantFile = -1;

    /**
     * Bitmask representing castling rights (4 bits: White kingside, queenside, Black kingside, queenside).
     */
    private int castlingRights = Bits.INITIAL_CASTLING_RIGHTS;

    /**
     * Number of half-moves since the last capture or pawn move, for the fifty-move rule.
     */
    private int fiftyMoveCounter = 0;

    /**
     * Checks if the specified side has any castling rights.
     *
     * @param isWhite true for White, false for Black
     * @return true if the side can castle kingside or queenside, false otherwise
     */
    public boolean hasCastlingRights(boolean isWhite) {
        return isKingsideCastlingAllowed(isWhite) || isQueensideCastlingAllowed(isWhite);
    }

    /**
     * Checks if kingside castling is allowed for the specified side.
     *
     * @param isWhite true for White, false for Black
     * @return true if kingside castling is allowed, false otherwise
     */
    public boolean isKingsideCastlingAllowed(boolean isWhite) {
        long kingsideMask = isWhite ? 0b0001 : 0b0100;
        return (castlingRights & kingsideMask) == kingsideMask;
    }

    /**
     * Checks if queenside castling is allowed for the specified side.
     *
     * @param isWhite true for White, false for Black
     * @return true if queenside castling is allowed, false otherwise
     */
    public boolean isQueensideCastlingAllowed(boolean isWhite) {
        long queensideMask = isWhite ? 0b0010 : 0b1000;
        return (castlingRights & queensideMask) == queensideMask;
    }

    /**
     * Creates a deep copy of this game state.
     *
     * @return a new GameState instance with identical values
     */
    public GameState copy() {
        return new GameState(zobristKey, pawnKey, capturedPiece, enPassantFile, castlingRights, fiftyMoveCounter);
    }
}