package com.utsab.chess_engine.core.board;

import com.utsab.chess_engine.core.utils.BoardUtils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Represents the current state of a chessboard, including piece positions, side to move, en passant rights,
 * castling rights, fifty-move counter, and move history. Uses bitboards for efficient piece representation
 * and provides methods to make and unmake moveEntities, essential for search and evaluation in a UCI-compatible
 * chess engine.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @see <a href="https://www.chessprogramming.org/Board_Representation">Chess Programming Wiki - Board Representation</a>
 * @since 2025
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Board {

    long whitePawns = BitboardConstants.WHITE_PAWNS_START;
    long whiteKnights = BitboardConstants.WHITE_KNIGHTS_START;
    long whiteBishops = BitboardConstants.WHITE_BISHOPS_START;
    long whiteRooks = BitboardConstants.WHITE_ROOKS_START;
    long whiteQueens = BitboardConstants.WHITE_QUEENS_START;
    long whiteKing = BitboardConstants.WHITE_KING_START;
    long blackPawns = BitboardConstants.BLACK_PAWNS_START;
    long blackKnights = BitboardConstants.BLACK_KNIGHTS_START;
    long blackBishops = BitboardConstants.BLACK_BISHOPS_START;
    long blackRooks = BitboardConstants.BLACK_ROOKS_START;
    long blackQueens = BitboardConstants.BLACK_QUEENS_START;
    long blackKing = BitboardConstants.BLACK_KING_START;

    long whitePieces;
    long blackPieces;
    long occupied;

    Piece[] pieceList = BoardUtils.getStartingPieceList();

    boolean isWhiteToMove = true;

    GameState gameState = new GameState();
    Deque<GameState> gameStateHistory = new ArrayDeque<>();
    Deque<Move> moveHistory = new ArrayDeque<>();

    /**
     * Constructs a new board with the starting position, initializing Zobrist keys and piece bitboards.
     */
    public Board() {
        gameState.setZobristKey(Zobrist.generateKey(this));
        gameState.setPawnKey(Zobrist.generatePawnKey(this));
        recalculatePieces();
    }

    /**
     * Updates the board state by applying the specified move, toggling piece bitboards and updating game state
     * (e.g., Zobrist keys, castling rights, en passant file). Handles special moveEntities like pawn double moveEntities,
     * castling, promotions, and en passant captures.
     *
     * @param move the move to apply
     */
    public void makeMove(Move move) {
        int startSquare = move.getStartSquare();
        int endSquare = move.getEndSquare();
        Piece piece = pieceList[startSquare];
        Piece capturedPiece = move.isEnPassant() ? Piece.PAWN : pieceList[endSquare];

        int newFiftyMoveCounter = gameState.getFiftyMoveCounter();
        int newEnPassantFile = -1;
        boolean resetFiftyMoveCounter = capturedPiece != null || Piece.PAWN.equals(piece);
        newFiftyMoveCounter = resetFiftyMoveCounter ? 0 : ++newFiftyMoveCounter;
        long zobrist = gameState.getZobristKey();
        long pawnZobrist = gameState.getPawnKey();

        if (move.isPawnDoubleMove()) {
            toggleSquares(piece, isWhiteToMove, startSquare, endSquare);
            pieceList[startSquare] = null;
            pieceList[endSquare] = Piece.PAWN;
            zobrist = Zobrist.updatePiece(zobrist, startSquare, Piece.PAWN, isWhiteToMove);
            zobrist = Zobrist.updatePiece(zobrist, endSquare, Piece.PAWN, isWhiteToMove);
            pawnZobrist = Zobrist.updatePiece(pawnZobrist, startSquare, Piece.PAWN, isWhiteToMove);
            pawnZobrist = Zobrist.updatePiece(pawnZobrist, endSquare, Piece.PAWN, isWhiteToMove);
            newEnPassantFile = BoardUtils.getFile(endSquare);
        } else if (move.isCastling()) {
            toggleSquares(Piece.KING, isWhiteToMove, startSquare, endSquare);
            pieceList[startSquare] = null;
            pieceList[endSquare] = Piece.KING;
            zobrist = Zobrist.updatePiece(zobrist, startSquare, Piece.KING, isWhiteToMove);
            zobrist = Zobrist.updatePiece(zobrist, endSquare, Piece.KING, isWhiteToMove);
            boolean isKingSide = BoardUtils.getFile(endSquare) == 6;
            int rookStartSquare = isKingSide ? (isWhiteToMove ? 7 : 63) : (isWhiteToMove ? 0 : 56);
            int rookEndSquare = isKingSide ? (isWhiteToMove ? 5 : 61) : (isWhiteToMove ? 3 : 59);
            toggleSquares(Piece.ROOK, isWhiteToMove, rookStartSquare, rookEndSquare);
            pieceList[rookStartSquare] = null;
            pieceList[rookEndSquare] = Piece.ROOK;
            zobrist = Zobrist.updatePiece(zobrist, rookStartSquare, Piece.ROOK, isWhiteToMove);
            zobrist = Zobrist.updatePiece(zobrist, rookEndSquare, Piece.ROOK, isWhiteToMove);
        } else if (move.isPromotion()) {
            toggleSquare(Piece.PAWN, isWhiteToMove, startSquare);
            toggleSquare(move.getPromotionPieceType(), isWhiteToMove, endSquare);
            pieceList[startSquare] = null;
            pieceList[endSquare] = move.getPromotionPieceType();
            zobrist = Zobrist.updatePiece(zobrist, startSquare, Piece.PAWN, isWhiteToMove);
            pawnZobrist = Zobrist.updatePiece(pawnZobrist, startSquare, Piece.PAWN, isWhiteToMove);
            if (capturedPiece != null) {
                toggleSquare(capturedPiece, !isWhiteToMove, endSquare);
                zobrist = Zobrist.updatePiece(zobrist, endSquare, capturedPiece, !isWhiteToMove);
                if (capturedPiece == Piece.PAWN) {
                    pawnZobrist = Zobrist.updatePiece(pawnZobrist, endSquare, capturedPiece, !isWhiteToMove);
                }
            }
            zobrist = Zobrist.updatePiece(zobrist, endSquare, move.getPromotionPieceType(), isWhiteToMove);
        } else if (move.isEnPassant()) {
            toggleSquares(piece, isWhiteToMove, startSquare, endSquare);
            int pawnSquare = isWhiteToMove ? endSquare - 8 : endSquare + 8;
            toggleSquare(Piece.PAWN, !isWhiteToMove, pawnSquare);
            pieceList[startSquare] = null;
            pieceList[pawnSquare] = null;
            pieceList[endSquare] = Piece.PAWN;
            zobrist = Zobrist.updatePiece(zobrist, startSquare, Piece.PAWN, isWhiteToMove);
            zobrist = Zobrist.updatePiece(zobrist, pawnSquare, Piece.PAWN, !isWhiteToMove);
            zobrist = Zobrist.updatePiece(zobrist, endSquare, Piece.PAWN, isWhiteToMove);
            pawnZobrist = Zobrist.updatePiece(pawnZobrist, startSquare, Piece.PAWN, isWhiteToMove);
            pawnZobrist = Zobrist.updatePiece(pawnZobrist, pawnSquare, Piece.PAWN, !isWhiteToMove);
            pawnZobrist = Zobrist.updatePiece(pawnZobrist, endSquare, Piece.PAWN, isWhiteToMove);
        } else {
            toggleSquares(piece, isWhiteToMove, startSquare, endSquare);
            if (capturedPiece != null) {
                toggleSquare(capturedPiece, !isWhiteToMove, endSquare);
                zobrist = Zobrist.updatePiece(zobrist, endSquare, capturedPiece, !isWhiteToMove);
                if (capturedPiece == Piece.PAWN) {
                    pawnZobrist = Zobrist.updatePiece(pawnZobrist, endSquare, capturedPiece, !isWhiteToMove);
                }
            }
            pieceList[startSquare] = null;
            pieceList[endSquare] = piece;
            zobrist = Zobrist.updatePiece(zobrist, startSquare, piece, isWhiteToMove);
            zobrist = Zobrist.updatePiece(zobrist, endSquare, piece, isWhiteToMove);
            if (piece == Piece.PAWN) {
                pawnZobrist = Zobrist.updatePiece(pawnZobrist, startSquare, piece, isWhiteToMove);
                pawnZobrist = Zobrist.updatePiece(pawnZobrist, endSquare, piece, isWhiteToMove);
            }
        }

        int newCastlingRights = calculateCastlingRights(startSquare, endSquare, piece);

        zobrist = Zobrist.updateCastlingRights(zobrist, gameState.getCastlingRights(), newCastlingRights);
        zobrist = Zobrist.updateEnPassantFile(zobrist, gameState.getEnPassantFile(), newEnPassantFile);
        zobrist = Zobrist.updateSideToMove(zobrist);

        GameState newGameState = new GameState(zobrist, pawnZobrist, capturedPiece, newEnPassantFile, newCastlingRights, newFiftyMoveCounter);
        gameStateHistory.push(gameState);
        gameState = newGameState;

        moveHistory.push(move);

        isWhiteToMove = !isWhiteToMove;
        recalculatePieces();
    }

    /**
     * Reverts the board state to before the last move, restoring piece bitboards and game state.
     **/
    public void unmakeMove() {
        isWhiteToMove = !isWhiteToMove;
        Move lastMove = moveHistory.pop();
        int startSquare = lastMove.getStartSquare();
        int endSquare = lastMove.getEndSquare();
        Piece piece = pieceAt(endSquare);

        if (lastMove.isCastling()) {
            toggleSquares(Piece.KING, isWhiteToMove, endSquare, startSquare);
            boolean isKingSide = BoardUtils.getFile(endSquare) == 6;
            int rookStartSquare = isKingSide ? (isWhiteToMove ? 5 : 61) : (isWhiteToMove ? 3 : 59);
            int rookEndSquare = isKingSide ? (isWhiteToMove ? 7 : 63) : (isWhiteToMove ? 0 : 56);
            toggleSquares(Piece.ROOK, isWhiteToMove, rookStartSquare, rookEndSquare);
            pieceList[startSquare] = Piece.KING;
            pieceList[endSquare] = null;
            pieceList[rookEndSquare] = Piece.ROOK;
            pieceList[rookStartSquare] = null;
        } else if (lastMove.isPromotion()) {
            toggleSquare(lastMove.getPromotionPieceType(), isWhiteToMove, endSquare);
            toggleSquare(Piece.PAWN, isWhiteToMove, startSquare);
            if (gameState.getCapturedPiece() != null) {
                toggleSquare(gameState.getCapturedPiece(), !isWhiteToMove, endSquare);
            }
            pieceList[startSquare] = Piece.PAWN;
            pieceList[endSquare] = gameState.getCapturedPiece();
        } else if (lastMove.isEnPassant()) {
            toggleSquares(Piece.PAWN, isWhiteToMove, endSquare, startSquare);
            int captureSquare = isWhiteToMove ? endSquare - 8 : endSquare + 8;
            toggleSquare(Piece.PAWN, !isWhiteToMove, captureSquare);
            pieceList[startSquare] = Piece.PAWN;
            pieceList[endSquare] = null;
            pieceList[captureSquare] = Piece.PAWN;
        } else {
            toggleSquares(piece, isWhiteToMove, endSquare, startSquare);
            if (gameState.getCapturedPiece() != null) {
                toggleSquare(gameState.getCapturedPiece(), !isWhiteToMove, endSquare);
            }
            pieceList[startSquare] = piece;
            pieceList[endSquare] = gameState.getCapturedPiece();
        }

        gameState = gameStateHistory.pop();
        recalculatePieces();
    }

    /**
     * Applies a null move, passing the turn to the opponent without moving pieces, used in null-move pruning.
     **/
    public void makeNullMove() {
        isWhiteToMove = !isWhiteToMove;
        long newZobristKey = Zobrist.updateKeyAfterNullMove(gameState.getZobristKey(), gameState.getEnPassantFile());
        GameState newGameState = new GameState(newZobristKey, gameState.getPawnKey(), null, -1, gameState.getCastlingRights(), 0);
        gameStateHistory.push(gameState);
        gameState = newGameState;
    }

    /**
     * Reverts a null move, restoring the previous board state.
     **/
    public void unmakeNullMove() {
        isWhiteToMove = !isWhiteToMove;
        gameState = gameStateHistory.pop();
    }

    /**
     * Toggles the presence of a piece on two squares (e.g., moving a piece from start to end).
     *
     * @param type        the piece type
     * @param isWhite     true for White pieces, false for Black
     * @param startSquare the starting square (0-63)
     * @param endSquare   the ending square (0-63)
     **/
    public void toggleSquares(Piece type, boolean isWhite, int startSquare, int endSquare) {
        long toggleMask = (1L << startSquare | 1L << endSquare);
        toggle(type, isWhite, toggleMask);
    }

    /**
     * Toggles the presence of a piece on a single square.
     *
     * @param type    the piece type
     * @param isWhite true for White pieces, false for Black
     * @param square  the square index (0-63)
     */
    public void toggleSquare(Piece type, boolean isWhite, int square) {
        long toggleMask = 1L << square;
        toggle(type, isWhite, toggleMask);
    }

    /**
     * Toggles piece bitboards based on piece type and color.
     *
     * @param type       the piece type
     * @param isWhite    true for White pieces, false for Black
     * @param toggleMask the bitboard mask to toggle
     */
    private void toggle(Piece type, boolean isWhite, long toggleMask) {
        switch (type) {
            case PAWN -> {
                if (isWhite) whitePawns ^= toggleMask;
                else blackPawns ^= toggleMask;
            }
            case KNIGHT -> {
                if (isWhite) whiteKnights ^= toggleMask;
                else blackKnights ^= toggleMask;
            }
            case BISHOP -> {
                if (isWhite) whiteBishops ^= toggleMask;
                else blackBishops ^= toggleMask;
            }
            case ROOK -> {
                if (isWhite) whiteRooks ^= toggleMask;
                else blackRooks ^= toggleMask;
            }
            case QUEEN -> {
                if (isWhite) whiteQueens ^= toggleMask;
                else blackQueens ^= toggleMask;
            }
            case KING -> {
                if (isWhite) whiteKing ^= toggleMask;
                else blackKing ^= toggleMask;
            }
        }
    }

    /**
     * Recalculates the combined bitboards for White pieces, Black pieces, and occupied squares.
     */
    public void recalculatePieces() {
        whitePieces = whitePawns | whiteKnights | whiteBishops | whiteRooks | whiteQueens | whiteKing;
        blackPieces = blackPawns | blackKnights | blackBishops | blackRooks | blackQueens | blackKing;
        occupied = whitePieces | blackPieces;
    }

    /**
     * Calculates new castling rights after a move, clearing rights if the king or rooks move.
     *
     * @param startSquare the starting square of the move
     * @param endSquare   the ending square of the move
     * @param pieceType   the type of piece moved
     * @return the updated castling rights bitmask
     */
    private int calculateCastlingRights(int startSquare, int endSquare, Piece pieceType) {
        int newCastlingRights = gameState.getCastlingRights();
        if (newCastlingRights == 0b0000) {
            return newCastlingRights;
        }
        if (Piece.KING.equals(pieceType)) {
            newCastlingRights &= isWhiteToMove ? BitboardConstants.CLEAR_WHITE_CASTLING_MASK : BitboardConstants.CLEAR_BLACK_CASTLING_MASK;
        }
        if (startSquare == 7 || endSquare == 7) {
            newCastlingRights &= BitboardConstants.CLEAR_WHITE_KINGSIDE_MASK;
        }
        if (startSquare == 63 || endSquare == 63) {
            newCastlingRights &= BitboardConstants.CLEAR_BLACK_KINGSIDE_MASK;
        }
        if (startSquare == 0 || endSquare == 0) {
            newCastlingRights &= BitboardConstants.CLEAR_WHITE_QUEENSIDE_MASK;
        }
        if (startSquare == 56 || endSquare == 56) {
            newCastlingRights &= BitboardConstants.CLEAR_BLACK_QUEENSIDE_MASK;
        }
        return newCastlingRights;
    }

    /**
     * Retrieves the piece at a given square.
     *
     * @param square the square index (0-63)
     * @return the piece at the square, or null if empty
     */
    public Piece pieceAt(int square) {
        return pieceList[square];
    }

    /**
     * Retrieves the bitboard for pawns of the specified color.
     *
     * @param isWhite true for White pawns, false for Black
     * @return the pawn bitboard
     */
    public long getPawns(boolean isWhite) {
        return isWhite ? whitePawns : blackPawns;
    }

    /**
     * Retrieves the bitboard for knights of the specified color.
     *
     * @param isWhite true for White knights, false for Black
     * @return the knight bitboard
     */
    public long getKnights(boolean isWhite) {
        return isWhite ? whiteKnights : blackKnights;
    }

    /**
     * Retrieves the bitboard for bishops of the specified color.
     *
     * @param isWhite true for White bishops, false for Black
     * @return the bishop bitboard
     */
    public long getBishops(boolean isWhite) {
        return isWhite ? whiteBishops : blackBishops;
    }

    /**
     * Retrieves the bitboard for rooks of the specified color.
     *
     * @param isWhite true for White rooks, false for Black
     * @return the rook bitboard
     */
    public long getRooks(boolean isWhite) {
        return isWhite ? whiteRooks : blackRooks;
    }

    /**
     * Retrieves the bitboard for queens of the specified color.
     *
     * @param isWhite true for White queens, false for Black
     * @return the queen bitboard
     */
    public long getQueens(boolean isWhite) {
        return isWhite ? whiteQueens : blackQueens;
    }

    /**
     * Retrieves the bitboard for the king of the specified color.
     *
     * @param isWhite true for White king, false for Black
     * @return the king bitboard
     */
    public long getKing(boolean isWhite) {
        return isWhite ? whiteKing : blackKing;
    }

    /**
     * Retrieves the bitboard for all pieces of the specified color.
     *
     * @param isWhite true for White pieces, false for Black
     * @return the bitboard representing all pieces of the specified color
     */
    public long getPieces(boolean isWhite) {
        return isWhite ? whitePieces : blackPieces;
    }

    /**
     * Checks if the specified side has non-pawn, non-king pieces remaining.
     * Useful for evaluating endgame conditions.
     *
     * @param isWhite true to check White pieces, false for Black
     * @return true if there are knights, bishops or rooks or queens remaining, false otherwise
     */
    public boolean hasPiecesRemaining(boolean isWhite) {
        return isWhite
                ? (whiteKnights | whiteBishops | whiteRooks | whiteQueens) != 0
                : (blackKnights | blackBishops | blackRooks | blackQueens) != 0;
    }
}