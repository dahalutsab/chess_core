package com.utsab.chess_engine.core.generation;

import com.utsab.chess_engine.core.board.*;
import com.utsab.chess_engine.core.generation.check.PinCalculator;
import com.utsab.chess_engine.core.generation.check.PinCalculator.PinData;
import com.utsab.chess_engine.core.generation.check.RayCalculator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates all the legal moveEntities in a given position.
 * Using a hybrid of pseudo-legal and legal move generation: first we calculate the bitboards for checking pieces and
 * pinned pieces. If there is a check, we filter out all moveEntities that do not resolve the check. Finally, we filter out all
 * moveEntities that leave the king in (a new) check.
 */
@Component
public class MoveGenerator implements MoveGeneration {

    private final PinCalculator pinCalculator = new PinCalculator();
    private final RayCalculator rayCalculator = new RayCalculator();

    private long checkersMask;
    private long pinMask;
    private long[] pinRayMasks;
    private long captureMask;
    private long pushMask;
    private MoveFilter filter;

    private List<Move> legalMoves;

    /**
     * Generates all legal moveEntities for the current board position.
     *
     * @param board The current board state.
     * @return A list of all legal moveEntities.
     */
    @Override
    public List<Move> generateMoves(Board board) {
        return generateMoves(board, MoveFilter.ALL);
    }

    /**
     * Generates legal moveEntities for the current board position based on the provided filter.
     *
     * @param board  The current board state.
     * @param filter The filter to apply to move generation.
     * @return A list of legal moveEntities filtered according to the given criteria.
     */
    @Override
    public List<Move> generateMoves(Board board, MoveFilter filter) {

        boolean isWhite = board.isWhiteToMove();
        int kingSquare = BitOperations.getNextBit(board.getKing(isWhite));
        this.filter = filter;

        // Initialize capture and push masks
        captureMask = BitboardConstants.ALL_SQUARES;
        pushMask = BitboardConstants.ALL_SQUARES;

        // Calculate pins and checks
        PinData pinData = pinCalculator.calculatePinMask(board, isWhite);
        pinMask = pinData.pinMask();
        pinRayMasks = pinData.pinRayMasks();
        checkersMask = calculateAttackerMask(board, isWhite, 1L << kingSquare);
        long checkersCount = BitOperations.countBits(checkersMask);

        legalMoves = new ArrayList<>();

        // Generate king moveEntities first
        generateKingMoves(board);

        if (checkersCount == 2) {
            // If we are in double-check, the only legal moveEntities are king moveEntities
            return legalMoves;
        }

        if (checkersCount == 1) {
            // If only one checker, we can evade check by capturing it
            captureMask = checkersMask;

            int checkerSquare = BitOperations.getNextBit(checkersMask);
            if (board.pieceAt(checkerSquare).isSlider()) {
                // If the piece giving check is a slider, we can evade check by blocking it
                pushMask = rayCalculator.rayBetween(checkerSquare, kingSquare);
            } else {
                // If the piece is not a slider, we can only evade check by capturing it
                // Therefore all non-capture 'push' moveEntities are illegal.
                pushMask = BitboardConstants.NO_SQUARES;
            }
        }

        // Generate all the other legal moveEntities using the capture and push masks
        generatePawnMoves(board);
        generateKnightMoves(board);
        generateAllSlidingMoves(board);
        generateCastlingMoves(board);

        return legalMoves;

    }

    /**
     * Checks if the specified side is in check.
     *
     * @param board   The current board state.
     * @param isWhite Indicates whether the side in question is white.
     * @return True if the specified side is in check, otherwise false.
     */
    @Override
    public boolean isCheck(Board board, boolean isWhite) {
        long king = board.getKing(isWhite);
        return isAttacked(board, isWhite, king);
    }

    private void generatePawnMoves(Board board) {

        boolean isWhite = board.isWhiteToMove();
        long pawns = board.getPawns(isWhite);
        if (pawns == 0) return;
        long opponents = board.getPieces(!isWhite);
        long occupied = board.getOccupied();
        long enPassantFile = BitOperations.getFileBitboard(board.getGameState().getEnPassantFile());
        int opponentKing = BitOperations.getNextBit(board.getKing(!isWhite));

        long filterMask = BitboardConstants.ALL_SQUARES;
        long promotionFilterMask = BitboardConstants.ALL_SQUARES;
        if (filter == MoveFilter.CAPTURES_ONLY) {
            filterMask = opponents;
            promotionFilterMask = opponents;
        } else if (filter == MoveFilter.CAPTURES_AND_CHECKS) {
            filterMask = opponents | MagicBitboards.pawnAttacks(1L << opponentKing, !isWhite);
            promotionFilterMask = opponents | MagicBitboards.bishopAttacks(opponentKing, occupied) | MagicBitboards.rookAttacks(opponentKing, occupied);
        }

        if (filter != MoveFilter.CAPTURES_ONLY) {

            long singleMoves = BitOperations.pawnSingleMoves(pawns, occupied, isWhite) & pushMask & filterMask;
            long doubleMoves = BitOperations.pawnDoubleMoves(pawns, occupied, isWhite) & pushMask & filterMask;

            while (singleMoves != 0) {
                int endSquare = BitOperations.getNextBit(singleMoves);
                int startSquare = isWhite ? endSquare - 8 : endSquare + 8;
                if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                    legalMoves.add(new Move(startSquare, endSquare));
                }
                singleMoves = BitOperations.popBit(singleMoves);
            }
            while (doubleMoves != 0) {
                int endSquare = BitOperations.getNextBit(doubleMoves);
                int startSquare = isWhite ? endSquare - 16 : endSquare + 16;
                if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                    legalMoves.add(new Move(startSquare, endSquare, Move.PAWN_DOUBLE_MOVE_FLAG));
                }
                doubleMoves = BitOperations.popBit(doubleMoves);
            }
        }

        long leftCaptures = BitOperations.pawnLeftCaptures(pawns, opponents, isWhite) & captureMask & filterMask;
        long rightCaptures = BitOperations.pawnRightCaptures(pawns, opponents, isWhite) & captureMask & filterMask;
        long leftEnPassants = BitOperations.pawnLeftEnPassants(pawns, enPassantFile, isWhite);
        long rightEnPassants = BitOperations.pawnRightEnPassants(pawns, enPassantFile, isWhite);
        long pushPromotions = BitOperations.pawnPushPromotions(pawns, occupied, isWhite) & pushMask & promotionFilterMask;
        long leftCapturePromotions = BitOperations.pawnLeftCapturePromotions(pawns, opponents, isWhite) & (captureMask | pushMask) & promotionFilterMask;
        long rightCapturePromotions = BitOperations.pawnRightCapturePromotions(pawns, opponents, isWhite) & (captureMask | pushMask) & promotionFilterMask;

        while (leftCaptures != 0) {
            int endSquare = BitOperations.getNextBit(leftCaptures);
            int startSquare = isWhite ? endSquare - 7 : endSquare + 9;
            if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                legalMoves.add(new Move(startSquare, endSquare));
            }
            leftCaptures = BitOperations.popBit(leftCaptures);
        }
        while (rightCaptures != 0) {
            int endSquare = BitOperations.getNextBit(rightCaptures);
            int startSquare = isWhite ? endSquare - 9 : endSquare + 7;
            if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                legalMoves.add(new Move(startSquare, endSquare));
            }
            rightCaptures = BitOperations.popBit(rightCaptures);
        }
        while (leftEnPassants != 0) {
            int endSquare = BitOperations.getNextBit(leftEnPassants);
            int startSquare = isWhite ? endSquare - 7 : endSquare + 9;
            // En passant is complicated; just test legality by making the move on the board and checking
            // whether the king is attacked.
            Move move = new Move(startSquare, endSquare, Move.EN_PASSANT_FLAG);
            if (!leavesKingInCheck(board, move, isWhite)) {
                legalMoves.add(move);
            }
            leftEnPassants = BitOperations.popBit(leftEnPassants);
        }
        while (rightEnPassants != 0) {
            int endSquare = BitOperations.getNextBit(rightEnPassants);
            int startSquare = isWhite ? endSquare - 9 : endSquare + 7;
            // En passant is complicated; just test legality by making the move on the board and checking
            // whether the king is attacked.
            Move move = new Move(startSquare, endSquare, Move.EN_PASSANT_FLAG);
            if (!leavesKingInCheck(board, move, isWhite)) {
                legalMoves.add(move);
            }
            rightEnPassants = BitOperations.popBit(rightEnPassants);
        }
        while (pushPromotions != 0) {
            int endSquare = BitOperations.getNextBit(pushPromotions);
            int startSquare = isWhite ? endSquare - 8 : endSquare + 8;
            if (!isPinned(startSquare)) {
                legalMoves.addAll(getPromotionMoves(startSquare, endSquare));
            }
            pushPromotions = BitOperations.popBit(pushPromotions);
        }
        while (leftCapturePromotions != 0) {
            int endSquare = BitOperations.getNextBit(leftCapturePromotions);
            int startSquare = isWhite ? endSquare - 7 : endSquare + 9;
            if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                legalMoves.addAll(getPromotionMoves(startSquare, endSquare));
            }
            leftCapturePromotions = BitOperations.popBit(leftCapturePromotions);
        }
        while (rightCapturePromotions != 0) {
            int endSquare = BitOperations.getNextBit(rightCapturePromotions);
            int startSquare = isWhite ? endSquare - 9 : endSquare + 7;
            if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                legalMoves.addAll(getPromotionMoves(startSquare, endSquare));
            }
            rightCapturePromotions = BitOperations.popBit(rightCapturePromotions);
        }

    }

    private void generateKnightMoves(Board board) {
        boolean isWhite = board.isWhiteToMove();
        long knights = board.getKnights(isWhite);
        if (knights == 0) return;
        long opponents = board.getPieces(!isWhite);

        // Initialize filter mask based on move filter type
        long filterMask = BitboardConstants.ALL_SQUARES;
        if (filter == MoveFilter.CAPTURES_ONLY) {
            filterMask = opponents;
        } else if (filter == MoveFilter.CAPTURES_AND_CHECKS) {
            int opponentKing = BitOperations.getNextBit(board.getKing(!isWhite));
            filterMask = opponents | MagicBitboards.knightAttacks(opponentKing);
        }

        // Exclude pinned knights from generating moveEntities
        long unpinnedKnights = knights & ~pinMask;

        // Generate legal knight moveEntities
        while (unpinnedKnights != 0) {
            int startSquare = BitOperations.getNextBit(unpinnedKnights);
            long possibleMoves = getKnightAttacks(board, startSquare, isWhite) & (pushMask | captureMask) & filterMask;
            while (possibleMoves != 0) {
                int endSquare = BitOperations.getNextBit(possibleMoves);
                legalMoves.add(new Move(startSquare, endSquare));
                possibleMoves = BitOperations.popBit(possibleMoves);
            }
            unpinnedKnights = BitOperations.popBit(unpinnedKnights);
        }
    }

    private void generateKingMoves(Board board) {
        boolean isWhite = board.isWhiteToMove();
        long king = board.getKing(isWhite);
        int startSquare = BitOperations.getNextBit(king);
        long friendlies = board.getPieces(isWhite);
        long opponents = board.getPieces(!isWhite);
        long filterMask = filter == MoveFilter.ALL ? BitboardConstants.ALL_SQUARES : opponents;
        long kingMoves = MagicBitboards.kingAttacks(startSquare) & ~friendlies & filterMask;

        // Temporarily remove the king from the board
        board.toggleSquare(Piece.KING, isWhite, startSquare);
        board.recalculatePieces();

        // Generate legal king moveEntities
        while (kingMoves != 0) {
            int endSquare = BitOperations.getNextBit(kingMoves);
            // Check if the end square is not attacked by the opponent
            if (!isAttacked(board, isWhite, 1L << endSquare)) {
                legalMoves.add(new Move(startSquare, endSquare));
            }
            kingMoves = BitOperations.popBit(kingMoves);
        }

        // Restore the king to its original position on the board
        board.toggleSquare(Piece.KING, isWhite, startSquare);
        board.recalculatePieces();
    }

    private void generateCastlingMoves(Board board) {
        if (filter != MoveFilter.ALL || checkersMask != 0) {
            return;
        }
        boolean isWhite = board.isWhiteToMove();
        long king = board.getKing(isWhite);
        int startSquare = BitOperations.getNextBit(king);
        long occupied = board.getOccupied();

        boolean isKingsideAllowed = board.getGameState().isKingsideCastlingAllowed(board.isWhiteToMove());
        if (isKingsideAllowed) {
            generateCastlingMove(board, isWhite, true, startSquare, occupied);
        }

        boolean isQueensideAllowed = board.getGameState().isQueensideCastlingAllowed(board.isWhiteToMove());
        if (isQueensideAllowed) {
            generateCastlingMove(board, isWhite, false, startSquare, occupied);
        }

    }

    private void generateCastlingMove(Board board, boolean isWhite, boolean isKingside, int startSquare, long occupied) {
        long travelSquares = getCastleTravelSquares(isWhite, isKingside);
        long blockedSquares = travelSquares & occupied;
        long safeSquares = getCastleSafeSquares(isWhite, isKingside);
        if (blockedSquares == 0 && !isAttacked(board, isWhite, safeSquares)) {
            int endSquare = getCastleEndSquare(isWhite, isKingside);
            legalMoves.add(new Move(startSquare, endSquare, Move.CASTLE_FLAG));
        }
    }

    private void generateAllSlidingMoves(Board board) {
        boolean isWhite = board.isWhiteToMove();
        long bishops = board.getBishops(isWhite);
        long rooks = board.getRooks(isWhite);
        long queens = board.getQueens(isWhite);
        if (filter == MoveFilter.ALL) {
            long diagonalSliders = bishops | queens;
            long orthogonalSliders = rooks | queens;
            generateSlidingMoves(board, diagonalSliders, false, true);
            generateSlidingMoves(board, orthogonalSliders, true, false);
        } else {
            generateSlidingMoves(board, bishops, false, true);
            generateSlidingMoves(board, rooks, true, false);
            generateSlidingMoves(board, queens, true, true);
        }
    }

    private void generateSlidingMoves(Board board, long sliders, boolean isOrthogonal, boolean isDiagonal) {
        boolean isWhite = board.isWhiteToMove();
        long opponents = board.getPieces(!isWhite);
        long occupied = board.getOccupied();

        while (sliders != 0) {
            int startSquare = BitOperations.getNextBit(sliders);
            long attackMask = getSlidingAttacks(board, startSquare, isWhite, isDiagonal, isOrthogonal);

            attackMask &= pushMask | captureMask;

            // Apply move filters
            if (filter == MoveFilter.CAPTURES_ONLY) {
                attackMask &= opponents;
            } else if (filter == MoveFilter.CAPTURES_AND_CHECKS) {
                attackMask &= getCaptureAndCheckMask(board, isWhite, opponents, occupied, isDiagonal, isOrthogonal);
            }

            // Handle pinned pieces
            if (isPinned(startSquare)) {
                attackMask &= pinRayMasks[startSquare];
            }

            sliders = BitOperations.popBit(sliders);
            while (attackMask != 0) {
                int endSquare = BitOperations.getNextBit(attackMask);
                legalMoves.add(new Move(startSquare, endSquare));
                attackMask = BitOperations.popBit(attackMask);
            }
        }
    }

    private long getCaptureAndCheckMask(Board board, boolean isWhite, long opponents, long occupied, boolean isDiagonal, boolean isOrthogonal) {
        int opponentKing = BitOperations.getNextBit(board.getKing(!isWhite));
        long filterMask = opponents;
        if (isDiagonal) {
            filterMask |= MagicBitboards.bishopAttacks(opponentKing, occupied);
        }
        if (isOrthogonal) {
            filterMask |= MagicBitboards.rookAttacks(opponentKing, occupied);
        }
        return filterMask;
    }

    public long getPawnAttacks(Board board, int square, boolean isWhite) {
        long attackMask = 0L;
        long squareBB = 1L << square;
        long friendlies = board.getPieces(isWhite);

        long leftCapture = isWhite ?
                BitOperations.shiftNorthWest(squareBB) &~ friendlies &~ BitboardConstants.FILE_H :
                BitOperations.shiftSouthWest(squareBB) &~ friendlies &~ BitboardConstants.FILE_H;
        attackMask |= leftCapture;

        long rightCapture = isWhite ?
                BitOperations.shiftNorthEast(squareBB) &~ friendlies &~ BitboardConstants.FILE_A :
                BitOperations.shiftSouthEast(squareBB) &~ friendlies &~ BitboardConstants.FILE_A;
        attackMask |= rightCapture;

        return attackMask;
    }

    public long getKnightAttacks(Board board, int square, boolean isWhite) {
        long friendlies = board.getPieces(isWhite);
        return MagicBitboards.knightAttacks(square) &~ friendlies;
    }

    public long getBishopAttacks(Board board, int square, boolean isWhite) {
        return getSlidingAttacks(board, square, isWhite, true, false);
    }

    public long getRookAttacks(Board board, int square, boolean isWhite) {
        return getSlidingAttacks(board, square, isWhite, false, true);
    }

    public long getQueenAttacks(Board board, int square, boolean isWhite) {
        return getSlidingAttacks(board, square, isWhite, true, true);
    }

    public long getKingAttacks(Board board, int square, boolean isWhite) {
        long friendlies = board.getPieces(isWhite);
        return MagicBitboards.kingAttacks(square) &~ friendlies;
    }

    private long getSlidingAttacks(Board board, int square, boolean isWhite, boolean isDiagonal, boolean isOrthogonal) {
        long attackMask = 0L;
        long occ = board.getOccupied();
        long friendlies = board.getPieces(isWhite);
        if (isOrthogonal) {
            attackMask |= MagicBitboards.rookAttacks(square, occ) &~ friendlies;
        }
        if (isDiagonal) {
            attackMask |= MagicBitboards.bishopAttacks(square, occ) &~ friendlies;
        }
        return attackMask;
    }

    private long calculateAttackerMask(Board board, boolean isWhite, long squareMask) {
        long attackerMask = 0L;
        while (squareMask != 0) {
            int square = BitOperations.getNextBit(squareMask);

            long opponentPawns = board.getPawns(!isWhite);
            long pawnAttackMask = getPawnAttacks(board, square, isWhite);
            attackerMask |= pawnAttackMask & opponentPawns;

            long opponentKnights = board.getKnights(!isWhite);
            long knightAttackMask = getKnightAttacks(board, square, isWhite);
            attackerMask |= knightAttackMask & opponentKnights;

            long opponentBishops = board.getBishops(!isWhite);
            long bishopAttackMask = getBishopAttacks(board, square, isWhite);
            attackerMask |= bishopAttackMask & opponentBishops;

            long opponentRooks = board.getRooks(!isWhite);
            long rookAttackMask = getRookAttacks(board, square, isWhite);
            attackerMask |= rookAttackMask & opponentRooks;

            long opponentQueens = board.getQueens(!isWhite);
            attackerMask |= (bishopAttackMask | rookAttackMask) & opponentQueens;

            long opponentKing = board.getKing(!isWhite);
            long kingAttackMask = getKingAttacks(board, square, isWhite);
            attackerMask |= kingAttackMask & opponentKing;

            squareMask = BitOperations.popBit(squareMask);
        }
        return attackerMask;
    }

    private boolean isAttacked(Board board, boolean isWhite, long squareMask) {
        while (squareMask != 0) {
            int square = BitOperations.getNextBit(squareMask);

            long opponentPawns = board.getPawns(!isWhite);
            if (opponentPawns != 0) {
                long pawnAttackMask = getPawnAttacks(board, square, isWhite);
                if ((pawnAttackMask & opponentPawns) != 0) {
                    return true;
                }
            }

            long opponentKnights = board.getKnights(!isWhite);
            if (opponentKnights != 0) {
                long knightAttackMask = getKnightAttacks(board, square, isWhite);
                if ((knightAttackMask & opponentKnights) != 0) {
                    return true;
                }
            }

            long opponentBishops = board.getBishops(!isWhite);
            long opponentQueens = board.getQueens(!isWhite);
            long diagonalSliders = opponentBishops | opponentQueens;
            if (diagonalSliders != 0) {
                long bishopAttackMask = getBishopAttacks(board, square, isWhite);
                if ((bishopAttackMask & diagonalSliders) != 0) {
                    return true;
                }
            }

            long opponentRooks = board.getRooks(!isWhite);
            long orthogonalSliders = opponentRooks | opponentQueens;
            if (orthogonalSliders != 0) {
                long rookAttackMask = getRookAttacks(board, square, isWhite);
                if ((rookAttackMask & orthogonalSliders) != 0) {
                    return true;
                }
            }

            long opponentKing = board.getKing(!isWhite);
            long kingAttackMask = getKingAttacks(board, square, isWhite);
            if ((kingAttackMask & opponentKing) != 0) {
                return true;
            }

            squareMask = BitOperations.popBit(squareMask);
        }
        return false;
    }

    private List<Move> getPromotionMoves(int startSquare, int endSquare) {
        return filter != MoveFilter.ALL ?
                List.of(new Move(startSquare, endSquare, Move.PROMOTE_TO_QUEEN_FLAG)) :
                List.of(new Move(startSquare, endSquare, Move.PROMOTE_TO_QUEEN_FLAG),
                        new Move(startSquare, endSquare, Move.PROMOTE_TO_ROOK_FLAG),
                        new Move(startSquare, endSquare, Move.PROMOTE_TO_BISHOP_FLAG),
                        new Move(startSquare, endSquare, Move.PROMOTE_TO_KNIGHT_FLAG));
    }


    private boolean leavesKingInCheck(Board board, Move move, boolean isWhite) {
        board.makeMove(move);
        int kingSquare = isWhite ? BitOperations.getNextBit(board.getWhiteKing()) : BitOperations.getNextBit(board.getBlackKing());
        boolean isAttacked = isAttacked(board, isWhite, 1L << kingSquare);
        board.unmakeMove();
        return isAttacked;
    }

    private boolean isPinned(int startSquare) {
        return (1L << startSquare & pinMask) != 0;
    }

    private boolean isMovingAlongPinRay(int startSquare, int endSquare) {
        long pinRay = pinRayMasks[startSquare];
        return (1L << endSquare & pinRay) != 0;
    }

    private long getCastleTravelSquares(boolean isWhite, boolean isKingside) {
        if (isKingside) return isWhite ? BitboardConstants.WHITE_KINGSIDE_CASTLE_TRAVEL_MASK : BitboardConstants.BLACK_KINGSIDE_CASTLE_TRAVEL_MASK;
        else return isWhite ? BitboardConstants.WHITE_QUEENSIDE_CASTLE_TRAVEL_MASK : BitboardConstants.BLACK_QUEENSIDE_CASTLE_TRAVEL_MASK;
    }

    private long getCastleSafeSquares(boolean isWhite, boolean isKingside) {
        if (isKingside) return isWhite ? BitboardConstants.WHITE_KINGSIDE_CASTLE_SAFE_MASK : BitboardConstants.BLACK_KINGSIDE_CASTLE_SAFE_MASK;
        else return isWhite ? BitboardConstants.WHITE_QUEENSIDE_CASTLE_SAFE_MASK : BitboardConstants.BLACK_QUEENSIDE_CASTLE_SAFE_MASK;
    }

    private int getCastleEndSquare(boolean isWhite, boolean isKingside) {
        if (isKingside) return isWhite ? 6 : 62;
        else return isWhite ? 2 : 58;
    }

}
