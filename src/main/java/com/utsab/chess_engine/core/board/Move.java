package com.utsab.chess_engine.core.board;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

/**
 * Represents a chess move, encoded as a 16-bit integer containing start square, end square, and special move flags.
 * Supports standard moveEntities, promotions, castling, en passant, and pawn double moveEntities, compatible with UCI protocols.
 * <p>
 * The encoding structure is:
 * <ul>
 *     <li>BitboardConstants 0-5: Start square (0-63)</li>
 *     <li>BitboardConstants 6-11: End square (0-63)</li>
 *     <li>BitboardConstants 12-15: Special move flags (e.g., promotion, castling)</li>
 * </ul>
 *
 * * @version 1.0
 * @since 2025
 * @author Utsab Dahal
 * @see <a href="https://www.chessprogramming.org/Encoding_Moves">Chess Programming Wiki - Encoding Moves</a>
 * @see <a href="https://github.com/SebLague/Chess-Coding-Adventure">Chess Coding Adventure</a>
 */
@Getter
@EqualsAndHashCode
public class Move {

    /**
     * Special move flag indicating no special conditions.
     */
    public static final short NO_FLAG = 0b0000;

    /**
     * Special move flag for en passant captures.
     */
    public static final short EN_PASSANT_FLAG = 0b0001;

    /**
     * Special move flag for castling moveEntities.
     */
    public static final short CASTLE_FLAG = 0b0010;

    /**
     * Special move flag for pawn double moveEntities.
     */
    public static final short PAWN_DOUBLE_MOVE_FLAG = 0b0011;

    /**
     * Special move flag for pawn promotion to queen.
     */
    public static final short PROMOTE_TO_QUEEN_FLAG = 0b0100;

    /**
     * Special move flag for pawn promotion to knight.
     */
    public static final short PROMOTE_TO_KNIGHT_FLAG = 0b0101;

    /**
     * Special move flag for pawn promotion to rook.
     */
    public static final short PROMOTE_TO_ROOK_FLAG = 0b0110;

    /**
     * Special move flag for pawn promotion to bishop.
     */
    public static final short PROMOTE_TO_BISHOP_FLAG = 0b0111;

    /**
     * Mask for extracting the start square from the move value.
     */
    public static final int START_SQUARE_MASK = 0b0000000000111111;

    /**
     * Mask for extracting the end square from the move value.
     */
    public static final int END_SQUARE_MASK = 0b0000111111000000;

    private final int value; // 16-bit integer encoding the move

    /**
     * Constructs a move with the specified encoded value.
     *
     * @param value the 16-bit encoded move value
     *     */
    public Move(int value) {
        this.value = value;
    }

    /**
     * Constructs a move from start and end squares, with no special flags.
     *
     * @param startSquare the starting square (0-63)
     * @param endSquare the ending square (0-63)
     *     */
    public Move(int startSquare, int endSquare) {
        this.value = (startSquare | endSquare << 6);
    }

    /**
     * Constructs a move with start square, end square, and special move flag.
     *
     * @param startSquare the starting square (0-63)
     * @param endSquare the ending square (0-63)
     * @param flag the special move flag (e.g., {@link #PROMOTE_TO_QUEEN_FLAG})
     *     */
    public Move(int startSquare, int endSquare, int flag) {
        this.value = startSquare | (endSquare << 6) | (flag << 12);
    }

    /**
     * Retrieves the starting square of the move.
     *
     * @return the start square index (0-63)
     *     */
    public int getStartSquare() {
        return value & START_SQUARE_MASK;
    }

    /**
     * Retrieves the ending square of the move.
     *
     * @return the end square index (0-63)
     *     */
    public int getEndSquare() {
        return (value & END_SQUARE_MASK) >>> 6;
    }

    /**
     * Retrieves the piece type for pawn promotion, if applicable.
     *
     * @return the promotion piece type, or null if not a promotion
     *     */
    public Piece getPromotionPieceType() {
        return switch (value >>> 12) {
            case PROMOTE_TO_QUEEN_FLAG -> Piece.QUEEN;
            case PROMOTE_TO_ROOK_FLAG -> Piece.ROOK;
            case PROMOTE_TO_BISHOP_FLAG -> Piece.BISHOP;
            case PROMOTE_TO_KNIGHT_FLAG -> Piece.KNIGHT;
            default -> null;
        };
    }

    /**
     * Retrieves the special move flag for a promotion to the specified piece type.
     *
     * @param pieceType the piece type to promote to, or null for no promotion
     * @return the corresponding promotion flag, or {@link #NO_FLAG} if not applicable
     *     */
    public static short getPromotionFlag(Piece pieceType) {
        if (pieceType == null) {
            return NO_FLAG;
        }
        return switch (pieceType) {
            case QUEEN -> PROMOTE_TO_QUEEN_FLAG;
            case ROOK -> PROMOTE_TO_ROOK_FLAG;
            case BISHOP -> PROMOTE_TO_BISHOP_FLAG;
            case KNIGHT -> PROMOTE_TO_KNIGHT_FLAG;
            default -> NO_FLAG;
        };
    }

    /**
     * Checks if the move is a pawn promotion.
     *
     * @return true if the move is a promotion, false otherwise
     *     */
    public boolean isPromotion() {
        return (value >>> 12) >= PROMOTE_TO_QUEEN_FLAG;
    }

    /**
     * Checks if the move is an en passant capture.
     *
     * @return true if the move is en passant, false otherwise
     *     */
    public boolean isEnPassant() {
        return (value >>> 12) == EN_PASSANT_FLAG;
    }

    /**
     * Checks if the move is a castling move.
     *
     * @return true if the move is castling, false otherwise
     *     */
    public boolean isCastling() {
        return (value >>> 12) == CASTLE_FLAG;
    }

    /**
     * Checks if the move is a pawn double move.
     *
     * @return true if the move is a pawn double move, false otherwise
     *     */
    public boolean isPawnDoubleMove() {
        return (value >>> 12) == PAWN_DOUBLE_MOVE_FLAG;
    }

    /**
     * Checks if this move matches another move, considering start/end squares and promotion type.
     *
     * @param move the move to compare against
     * @return true if the moveEntities match, false otherwise
     *     */
    public boolean matches(Move move) {
        if (move == null) return false;
        boolean squareMatch = getStartSquare() == move.getStartSquare() && getEndSquare() == move.getEndSquare();
        boolean promotionMatch = Optional.ofNullable(getPromotionPieceType())
                .map(piece -> piece.equals(move.getPromotionPieceType()))
                .orElse(true);
        return squareMatch && promotionMatch;
    }
}