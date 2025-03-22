package com.utsab.chess_engine.board;

import java.util.logging.Logger;

/**
 * Represents a chess position using bitboards with high efficiency, supporting all chess rules
 * including castling, en passant, pawn promotion, and draw detection.
 */
public class Bitboard {
    private static final Logger logger = Logger.getLogger(Bitboard.class.getName());
    private long[] pieceBitboards; // 0-5: white P,N,B,R,Q,K; 6-11: black p,n,b,r,q,k
    private long occupied;        // All occupied squares
    private long whitePieces;     // All white pieces
    private long blackPieces;     // All black pieces
    private boolean whiteToMove;  // Whose turn it is
    private int castlingRights;   // Bits: KQkq (0b1111 = all available)
    private int enPassantSquare;  // Square behind double pawn push, -1 if none
    private int halfMoveClock;    // Moves since last pawn move or capture
    private int fullMoveNumber;   // Incremented after Black's move
    private long zobristHash;     // Zobrist hash for threefold repetition

    // Zobrist keys for efficient hashing
    private static final long[][] ZOBRIST_PIECE_KEYS = new long[12][64];
    private static final long[] ZOBRIST_CASTLING_KEYS = new long[16]; // 0b0000 to 0b1111
    private static final long[] ZOBRIST_EN_PASSANT_KEYS = new long[64];
    private static final long ZOBRIST_TURN_KEY;

    static {
        // Initialize Zobrist keys with pseudo-random values (simplified here)
        java.util.Random rand = new java.util.Random(12345); // Fixed seed for reproducibility
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 64; j++) {
                ZOBRIST_PIECE_KEYS[i][j] = rand.nextLong();
            }
        }
        for (int i = 0; i < 16; i++) {
            ZOBRIST_CASTLING_KEYS[i] = rand.nextLong();
        }
        for (int i = 0; i < 64; i++) {
            ZOBRIST_EN_PASSANT_KEYS[i] = rand.nextLong();
        }
        ZOBRIST_TURN_KEY = rand.nextLong();
    }

    /**
     * Constructs an empty Bitboard with default values.
     */
    public Bitboard() {
        pieceBitboards = new long[12];
        occupied = 0L;
        whitePieces = 0L;
        blackPieces = 0L;
        whiteToMove = true;
        castlingRights = 0b1111;
        enPassantSquare = -1;
        halfMoveClock = 0;
        fullMoveNumber = 1;
        zobristHash = initializeZobristHash();
    }

    // Complete Getters
    public long getPieceBitboard(int index) { return pieceBitboards[index]; }
    public long getOccupied() { return occupied; }
    public long getWhitePieces() { return whitePieces; }
    public long getBlackPieces() { return blackPieces; }
    public boolean isWhiteToMove() { return whiteToMove; }
    public int getCastlingRights() { return castlingRights; }
    public int getEnPassantSquare() { return enPassantSquare; }
    public int getHalfMoveClock() { return halfMoveClock; }
    public int getFullMoveNumber() { return fullMoveNumber; }
    public long getZobristHash() { return zobristHash; }

    // Complete Setters
    public void setPieceBitboard(int index, long value) {
        if (index < 0 || index >= 12) throw new IllegalArgumentException("Invalid piece index: " + index);
        pieceBitboards[index] = value;
        updateDerivedBitboards();
    }

    public void setOccupied(long occupied) {
        this.occupied = occupied;
    }

    public void setWhitePieces(long whitePieces) {
        this.whitePieces = whitePieces;
    }

    public void setBlackPieces(long blackPieces) {
        this.blackPieces = blackPieces;
    }

    public void setWhiteToMove(boolean whiteToMove) {
        if (this.whiteToMove != whiteToMove) {
            this.whiteToMove = whiteToMove;
            zobristHash ^= ZOBRIST_TURN_KEY; // Update hash when turn changes
        }
    }

    public void setCastlingRights(int castlingRights) {
        if (castlingRights < 0 || castlingRights > 0b1111) throw new IllegalArgumentException("Invalid castling rights: " + castlingRights);
        zobristHash ^= ZOBRIST_CASTLING_KEYS[this.castlingRights]; // Remove old
        this.castlingRights = castlingRights;
        zobristHash ^= ZOBRIST_CASTLING_KEYS[this.castlingRights]; // Add new
    }

    public void setEnPassantSquare(int square) {
        if (square < -1 || square >= 64) throw new IllegalArgumentException("Invalid en passant square: " + square);
        if (this.enPassantSquare != -1) {
            zobristHash ^= ZOBRIST_EN_PASSANT_KEYS[this.enPassantSquare]; // Remove old
        }
        this.enPassantSquare = square;
        if (square != -1) {
            zobristHash ^= ZOBRIST_EN_PASSANT_KEYS[square]; // Add new
        }
    }

    public void setHalfMoveClock(int halfMoveClock) {
        if (halfMoveClock < 0) throw new IllegalArgumentException("Half-move clock cannot be negative: " + halfMoveClock);
        this.halfMoveClock = halfMoveClock;
    }

    public void setFullMoveNumber(int fullMoveNumber) {
        if (fullMoveNumber < 1) throw new IllegalArgumentException("Full move number must be at least 1: " + fullMoveNumber);
        this.fullMoveNumber = fullMoveNumber;
    }

    public void setZobristHash(long zobristHash) {
        this.zobristHash = zobristHash;
    }

    /**
     * Records move details for undoing.
     */
    public record MoveInfo(int movingPieceIndex, int fromSquare, int toSquare, int capturedPieceIndex,
                           boolean previousWhiteToMove, int prevCastlingRights, int prevEnPassantSquare,
                           int prevHalfMoveClock, long prevZobristHash, int promotionPieceIndex) {}

    /**
     * Makes a move on the board, updating all state including special moves.
     *
     * @param move the move to make
     * @return MoveInfo for undoing the move
     * @throws IllegalStateException if no piece exists at the from square
     */
    public MoveInfo makeMove(Move move) {
        int from = move.fromSquare();
        int to = move.toSquare();
        int movingPieceIndex = getPieceIndexAtSquare(from);
        int capturedPieceIndex = getPieceIndexAtSquare(to);
        int promotionPieceIndex = -1;

        if (movingPieceIndex == -1) throw new IllegalStateException("No piece at square " + from);

        MoveInfo moveInfo = new MoveInfo(movingPieceIndex, from, to, capturedPieceIndex, whiteToMove,
                castlingRights, enPassantSquare, halfMoveClock, zobristHash, promotionPieceIndex);

        // Update Zobrist hash: Remove piece from 'from' square
        if (from >= 0 && from < 64) { // Guard
            zobristHash ^= ZOBRIST_PIECE_KEYS[movingPieceIndex][from];
        }

        // Handle pawn moves
        boolean isPawnMove = (movingPieceIndex == 0 || movingPieceIndex == 6);
        if (isPawnMove) {
            halfMoveClock = 0;
            if (Math.abs(to - from) == 16) { // Double pawn push
                enPassantSquare = (from + to) / 2;
                if (enPassantSquare >= 0 && enPassantSquare < 64) { // Guard
                    zobristHash ^= ZOBRIST_EN_PASSANT_KEYS[enPassantSquare];
                }
            } else if (enPassantSquare != -1) {
                if (enPassantSquare >= 0 && enPassantSquare < 64) { // Guard
                    zobristHash ^= ZOBRIST_EN_PASSANT_KEYS[enPassantSquare];
                }
                enPassantSquare = -1;
            }
            // Pawn promotion
            if ((to >= 56 && whiteToMove) || (to <= 7 && !whiteToMove)) {
                promotionPieceIndex = whiteToMove ? 4 : 10; // Default to queen
                pieceBitboards[movingPieceIndex] &= ~(1L << from);
                pieceBitboards[promotionPieceIndex] |= (1L << to);
                if (to >= 0 && to < 64) { // Guard
                    zobristHash ^= ZOBRIST_PIECE_KEYS[promotionPieceIndex][to];
                }
            }
        } else {
            halfMoveClock++;
            if (enPassantSquare != -1) {
                if (enPassantSquare >= 0 && enPassantSquare < 64) { // Guard
                    zobristHash ^= ZOBRIST_EN_PASSANT_KEYS[enPassantSquare];
                }
                enPassantSquare = -1;
            }
        }

        // Handle capture
        if (capturedPieceIndex != -1) {
            pieceBitboards[capturedPieceIndex] &= ~(1L << to);
            occupied &= ~(1L << to);
            if (capturedPieceIndex < 6) whitePieces &= ~(1L << to);
            else blackPieces &= ~(1L << to);
            halfMoveClock = 0;
            if (to >= 0 && to < 64) { // Guard
                zobristHash ^= ZOBRIST_PIECE_KEYS[capturedPieceIndex][to];
            }
        } else if (to == enPassantSquare && isPawnMove) { // En passant capture
            int capturedPawnSquare = whiteToMove ? to - 8 : to + 8;
            capturedPieceIndex = whiteToMove ? 6 : 0;
            if (capturedPawnSquare >= 0 && capturedPawnSquare < 64) { // Guard
                pieceBitboards[capturedPieceIndex] &= ~(1L << capturedPawnSquare);
                occupied &= ~(1L << capturedPawnSquare);
                if (capturedPieceIndex < 6) whitePieces &= ~(1L << capturedPawnSquare);
                else blackPieces &= ~(1L << capturedPawnSquare);
                zobristHash ^= ZOBRIST_PIECE_KEYS[capturedPieceIndex][capturedPawnSquare];
            }
        }

        // Move piece (if not promotion)
        if (promotionPieceIndex == -1) {
            pieceBitboards[movingPieceIndex] &= ~(1L << from);
            pieceBitboards[movingPieceIndex] |= (1L << to);
            if (to >= 0 && to < 64) { // Guard
                zobristHash ^= ZOBRIST_PIECE_KEYS[movingPieceIndex][to];
            }
        }
        occupied = (occupied & ~(1L << from)) | (1L << to);
        if (movingPieceIndex < 6) {
            whitePieces = (whitePieces & ~(1L << from)) | (1L << to);
        } else {
            blackPieces = (blackPieces & ~(1L << from)) | (1L << to);
        }

        // Castling rights update
        int oldCastlingRights = castlingRights;
        if (movingPieceIndex == 5) { // White king
            castlingRights &= ~0b1100;
        } else if (movingPieceIndex == 11) { // Black king
            castlingRights &= ~0b0011;
        } else if (from == 0 || to == 0) { // a1 rook
            castlingRights &= ~0b1000;
        } else if (from == 7 || to == 7) { // h1 rook
            castlingRights &= ~0b0100;
        } else if (from == 56 || to == 56) { // a8 rook
            castlingRights &= ~0b0010;
        } else if (from == 63 || to == 63) { // h8 rook
            castlingRights &= ~0b0001;
        }
        if (oldCastlingRights != castlingRights) {
            zobristHash ^= ZOBRIST_CASTLING_KEYS[oldCastlingRights];
            zobristHash ^= ZOBRIST_CASTLING_KEYS[castlingRights];
        }

        // Castling move
        if ((movingPieceIndex == 5 || movingPieceIndex == 11) && Math.abs(from - to) == 2) {
            int rookFrom = (to > from) ? (from + 3) : (from - 4);
            int rookTo = (to > from) ? (from + 1) : (from - 1);
            int rookIndex = (movingPieceIndex == 5) ? 3 : 9;
            if (rookFrom >= 0 && rookFrom < 64 && rookTo >= 0 && rookTo < 64) { // Guard
                pieceBitboards[rookIndex] &= ~(1L << rookFrom);
                pieceBitboards[rookIndex] |= (1L << rookTo);
                occupied = (occupied & ~(1L << rookFrom)) | (1L << rookTo);
                if (rookIndex < 6) whitePieces = (whitePieces & ~(1L << rookFrom)) | (1L << rookTo);
                else blackPieces = (blackPieces & ~(1L << rookFrom)) | (1L << rookTo);
                zobristHash ^= ZOBRIST_PIECE_KEYS[rookIndex][rookFrom];
                zobristHash ^= ZOBRIST_PIECE_KEYS[rookIndex][rookTo];
            }
        }

        whiteToMove = !whiteToMove;
        zobristHash ^= ZOBRIST_TURN_KEY;
        if (!whiteToMove) fullMoveNumber++;

        logger.info("Made move: " + move + "\n" + this);
        return moveInfo;
    }

    /**
     * Undoes a move, restoring the board to its previous state.
     *
     * @param moveInfo the move information to undo
     */
    public void undoMove(MoveInfo moveInfo) {
        int from = moveInfo.fromSquare();
        int to = moveInfo.toSquare();
        int movingPieceIndex = moveInfo.movingPieceIndex();
        int capturedPieceIndex = moveInfo.capturedPieceIndex();
        int promotionPieceIndex = moveInfo.promotionPieceIndex();

        // Undo promotion
        if (promotionPieceIndex != -1) {
            pieceBitboards[promotionPieceIndex] &= ~(1L << to);
            pieceBitboards[movingPieceIndex] |= (1L << from);
        } else {
            pieceBitboards[movingPieceIndex] &= ~(1L << to);
            pieceBitboards[movingPieceIndex] |= (1L << from);
        }

        occupied = (occupied & ~(1L << to)) | (1L << from);
        if (movingPieceIndex < 6) {
            whitePieces = (whitePieces & ~(1L << to)) | (1L << from);
        } else {
            blackPieces = (blackPieces & ~(1L << to)) | (1L << from);
        }

        // Restore captured piece
        if (capturedPieceIndex != -1) {
            int restoreSquare = (to == moveInfo.prevEnPassantSquare() && (movingPieceIndex == 0 || movingPieceIndex == 6))
                    ? (whiteToMove ? to + 8 : to - 8) : to;
            pieceBitboards[capturedPieceIndex] |= (1L << restoreSquare);
            occupied |= (1L << restoreSquare);
            if (capturedPieceIndex < 6) whitePieces |= (1L << restoreSquare);
            else blackPieces |= (1L << restoreSquare);
        }

        // Undo castling
        if ((movingPieceIndex == 5 || movingPieceIndex == 11) && Math.abs(from - to) == 2) {
            int rookFrom = (to > from) ? (from + 3) : (from - 4);
            int rookTo = (to > from) ? (from + 1) : (from - 1);
            int rookIndex = (movingPieceIndex == 5) ? 3 : 9;
            pieceBitboards[rookIndex] &= ~(1L << rookTo);
            pieceBitboards[rookIndex] |= (1L << rookFrom);
            occupied = (occupied & ~(1L << rookTo)) | (1L << rookFrom);
            if (rookIndex < 6) whitePieces = (whitePieces & ~(1L << rookTo)) | (1L << rookFrom);
            else blackPieces = (blackPieces & ~(1L << rookTo)) | (1L << rookFrom);
        }

        // Restore previous state
        whiteToMove = moveInfo.previousWhiteToMove();
        castlingRights = moveInfo.prevCastlingRights();
        enPassantSquare = moveInfo.prevEnPassantSquare();
        halfMoveClock = moveInfo.prevHalfMoveClock();
        zobristHash = moveInfo.prevZobristHash();
        if (whiteToMove) fullMoveNumber--;

        logger.info("Undid move, restored state:\n" + this);
    }

    /**
     * Gets the index of the piece at the specified square.
     *
     * @param square the square to check (0-63)
     * @return the piece index (0-11) or -1 if empty
     */
    public int getPieceIndexAtSquare(int square) {
        long mask = 1L << square;
        for (int i = 0; i < 12; i++) {
            if ((pieceBitboards[i] & mask) != 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if the king of the specified color is in check.
     *
     * @param white true to check White's king, false for Black's
     * @return true if the king is in check, false otherwise
     */
    public boolean isInCheck(boolean white) {
        int kingSquare = Long.numberOfTrailingZeros(pieceBitboards[white ? 5 : 11]);
        long opponents = white ? blackPieces : whitePieces;

        // Knight attacks
        if ((MoveGenerator.KNIGHT_ATTACKS[kingSquare] & pieceBitboards[white ? 7 : 1]) != 0) return true;

        // Bishop/Queen diagonal attacks
        long bishopAttacks = MoveGenerator.getBishopAttacks(kingSquare, occupied);
        if ((bishopAttacks & (pieceBitboards[white ? 8 : 2] | pieceBitboards[white ? 10 : 4])) != 0) return true;

        // Rook/Queen straight attacks
        long rookAttacks = MoveGenerator.getRookAttacks(kingSquare, occupied);
        if ((rookAttacks & (pieceBitboards[white ? 9 : 3] | pieceBitboards[white ? 10 : 4])) != 0) return true;

        // Pawn attacks
        long pawnAttacks = white ?
                (((1L << (kingSquare - 9)) & ~0x8080808080808080L) | ((1L << (kingSquare - 7)) & ~0x0101010101010100L)) :
                (((1L << (kingSquare + 9)) & ~0x0101010101010100L) | ((1L << (kingSquare + 7)) & ~0x8080808080808080L));
        return (pawnAttacks & pieceBitboards[white ? 6 : 0]) != 0;
    }

    /**
     * Updates derived bitboards (occupied, whitePieces, blackPieces) based on pieceBitboards.
     */
    private void updateDerivedBitboards() {
        occupied = 0L;
        whitePieces = 0L;
        blackPieces = 0L;
        for (int i = 0; i < 6; i++) {
            whitePieces |= pieceBitboards[i];
            blackPieces |= pieceBitboards[i + 6];
        }
        occupied = whitePieces | blackPieces;
    }

    /**
     * Initializes the Zobrist hash based on the current board state.
     *
     * @return the initial Zobrist hash
     */
    private long initializeZobristHash() {
        long hash = 0L;
        for (int i = 0; i < 12; i++) {
            long pieces = pieceBitboards[i];
            while (pieces != 0) {
                int square = Long.numberOfTrailingZeros(pieces);
                hash ^= ZOBRIST_PIECE_KEYS[i][square];
                pieces &= pieces - 1;
            }
        }
        hash ^= ZOBRIST_CASTLING_KEYS[castlingRights];
        if (enPassantSquare != -1) hash ^= ZOBRIST_EN_PASSANT_KEYS[enPassantSquare];
        if (whiteToMove) hash ^= ZOBRIST_TURN_KEY;
        return hash;
    }

    /**
     * Provides a string representation of the board.
     *
     * @return a formatted string showing the board state
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            sb.append(rank + 1).append(" ");
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                char piece = '.';
                for (int i = 0; i < 12; i++) {
                    if ((pieceBitboards[i] & (1L << square)) != 0) {
                        piece = "PNBRQKpnbrqk".charAt(i);
                        break;
                    }
                }
                sb.append(piece).append(" ");
            }
            sb.append("\n");
        }
        sb.append("  a b c d e f g h\n");
        sb.append("Turn: ").append(whiteToMove ? "White" : "Black").append("\n");
        sb.append("Castling: ").append(Integer.toBinaryString(castlingRights)).append("\n");
        sb.append("En Passant: ").append(enPassantSquare == -1 ? "-" : enPassantSquare).append("\n");
        sb.append("Half-move clock: ").append(halfMoveClock).append("\n");
        sb.append("Full move number: ").append(fullMoveNumber);
        return sb.toString();
    }

    /**
     * Creates a deep copy of the Bitboard.
     *
     * @return a new Bitboard instance with identical state
     */
    public Bitboard clone() {
        Bitboard copy = new Bitboard();
        System.arraycopy(pieceBitboards, 0, copy.pieceBitboards, 0, 12);
        copy.occupied = occupied;
        copy.whitePieces = whitePieces;
        copy.blackPieces = blackPieces;
        copy.whiteToMove = whiteToMove;
        copy.castlingRights = castlingRights;
        copy.enPassantSquare = enPassantSquare;
        copy.halfMoveClock = halfMoveClock;
        copy.fullMoveNumber = fullMoveNumber;
        copy.zobristHash = zobristHash;
        return copy;
    }
}