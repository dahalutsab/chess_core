package com.utsab.chess_engine.core.chess.generation;

import com.utsab.chess_engine.core.chess.board.Bits;
import com.utsab.chess_engine.core.chess.board.BitwiseOperations;
import com.utsab.chess_engine.core.chess.utils.BoardUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class for generating and using magic bitboards for efficient move generation of sliding pieces (rooks and bishops)
 * in a chess engine. This class provides precomputed attack tables, magic numbers, and methods to calculate attack and movement
 * masks for rooks, bishops, knights, kings, and pawns. It leverages magic bitboards to optimize legal move generation by
 * reducing the computational overhead of calculating sliding piece moves during gameplay.
 * <p>
 * The class includes:
 * <ul>
 *   <li>Precomputed attack tables for knights and kings, stored as bitboards for each square.</li>
 *   <li>Magic bitboard tables and lookup structures for rooks and bishops to handle sliding moves efficiently.</li>
 *   <li>Methods for generating pawn attacks, movement masks, and attack masks for all piece types.</li>
 *   <li>Utility methods for initializing magic lookup tables used in move generation.</li>
 * </ul>
 * <p>
 * Magic bitboards use a hashing technique to map occupancy patterns to precomputed attack sets, enabling fast lookup of valid
 * moves for sliding pieces. This implementation ensures compatibility with the UCI protocol and existing chess engine logic.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 */
public class MagicBitboards {

    /**
     * Precomputed attack bitboards for knights from each square on the board. Each entry represents the possible
     * destinations a knight can move to from the corresponding square, encoded as a bitboard.
     */
    public static final long[] KNIGHT_ATTACKS = new long[] {
            0x0000000000020400L, 0x0000000000050800L, 0x00000000000a1100L, 0x0000000000142200L,
            0x0000000000284400L, 0x0000000000508800L, 0x0000000000a01000L, 0x0000000000402000L,
            0x0000000002040004L, 0x0000000005080008L, 0x000000000a110011L, 0x0000000014220022L,
            0x0000000028440044L, 0x0000000050880088L, 0x00000000a0100010L, 0x0000000040200020L,
            0x0000000204000402L, 0x0000000508000805L, 0x0000000a1100110aL, 0x0000001422002214L,
            0x0000002844004428L, 0x0000005088008850L, 0x000000a0100010a0L, 0x0000004020002040L,
            0x0000020400040200L, 0x0000050800080500L, 0x00000a1100110a00L, 0x0000142200221400L,
            0x0000284400442800L, 0x0000508800885000L, 0x0000a0100010a000L, 0x0000402000204000L,
            0x0002040004020000L, 0x0005080008050000L, 0x000a1100110a0000L, 0x0014220022140000L,
            0x0028440044280000L, 0x0050880088500000L, 0x00a0100010a00000L, 0x0040200020400000L,
            0x0204000402000000L, 0x0508000805000000L, 0x0a1100110a000000L, 0x1422002214000000L,
            0x2844004428000000L, 0x5088008850000000L, 0xa0100010a0000000L, 0x4020002040000000L,
            0x0400040200000000L, 0x0800080500000000L, 0x1100110a00000000L, 0x2200221400000000L,
            0x4400442800000000L, 0x8800885000000000L, 0x100010a000000000L, 0x2000204000000000L,
            0x0004020000000000L, 0x0008050000000000L, 0x00110a0000000000L, 0x0022140000000000L,
            0x0044280000000000L, 0x0088500000000000L, 0x0010a00000000000L, 0x0020400000000000L
    };

    /**
     * Precomputed attack bitboards for kings from each square on the board. Each entry represents the possible
     * destinations a king can move to (one square in any direction) from the corresponding square, encoded as a bitboard.
     */
    public static final long[] KING_ATTACKS = new long[] {
            0x0000000000000302L, 0x0000000000000705L, 0x0000000000000e0aL, 0x0000000000001c14L,
            0x0000000000003828L, 0x0000000000007050L, 0x000000000000e0a0L, 0x000000000000c040L,
            0x0000000000030203L, 0x0000000000070507L, 0x00000000000e0a0eL, 0x00000000001c141cL,
            0x0000000000382838L, 0x0000000000705070L, 0x0000000000e0a0e0L, 0x0000000000c040c0L,
            0x0000000003020300L, 0x0000000007050700L, 0x000000000e0a0e00L, 0x000000001c141c00L,
            0x0000000038283800L, 0x0000000070507000L, 0x00000000e0a0e000L, 0x00000000c040c000L,
            0x0000000302030000L, 0x0000000705070000L, 0x0000000e0a0e0000L, 0x0000001c141c0000L,
            0x0000003828380000L, 0x0000007050700000L, 0x000000e0a0e00000L, 0x000000c040c00000L,
            0x0000030203000000L, 0x0000070507000000L, 0x00000e0a0e000000L, 0x00001c141c000000L,
            0x0000382838000000L, 0x0000705070000000L, 0x0000e0a0e0000000L, 0x0000c040c0000000L,
            0x0003020300000000L, 0x0007050700000000L, 0x000e0a0e00000000L, 0x001c141c00000000L,
            0x0038283800000000L, 0x0070507000000000L, 0x00e0a0e000000000L, 0x00c040c000000000L,
            0x0302030000000000L, 0x0705070000000000L, 0x0e0a0e0000000000L, 0x1c141c0000000000L,
            0x3828380000000000L, 0x7050700000000000L, 0xe0a0e00000000000L, 0xc040c00000000000L,
            0x0203000000000000L, 0x0507000000000000L, 0x0a0e000000000000L, 0x141c000000000000L,
            0x2838000000000000L, 0x5070000000000000L, 0xa0e0000000000000L, 0x40c0000000000000L
    };

    /**
     * Precomputed magic numbers for rooks, used to hash occupancy patterns to indices in the rook attack table.
     * Each entry corresponds to a square on the board.
     */
    public static final long[] ROOK_MAGICS = new long[] {
            0x0080001020400080L, 0x0040001000200040L, 0x0080081000200080L, 0x0080040800100080L,
            0x0080020400080080L, 0x0080010200040080L, 0x0080008001000200L, 0x0080002040800100L,
            0x0000800020400080L, 0x0000400020005000L, 0x0000801000200080L, 0x0000800800100080L,
            0x0000800400080080L, 0x0000800200040080L, 0x0000800100020080L, 0x0000800040800100L,
            0x0000208000400080L, 0x0000404000201000L, 0x0000808010002000L, 0x0000808008001000L,
            0x0000808004000800L, 0x0000808002000400L, 0x0000010100020004L, 0x0000020000408104L,
            0x0000208080004000L, 0x0000200040005000L, 0x0000100080200080L, 0x0000080080100080L,
            0x0000040080080080L, 0x0000020080040080L, 0x0000010080800200L, 0x0000800080004100L,
            0x0000204000800080L, 0x0000200040401000L, 0x0000100080802000L, 0x0000080080801000L,
            0x0000040080800800L, 0x0000020080800400L, 0x0000020001010004L, 0x0000800040800100L,
            0x0000204000808000L, 0x0000200040008080L, 0x0000100020008080L, 0x0000080010008080L,
            0x0000040008008080L, 0x0000020004008080L, 0x0000010002008080L, 0x0000004081020004L,
            0x0000204000800080L, 0x0000200040008080L, 0x0000100020008080L, 0x0000080010008080L,
            0x0000040008008080L, 0x0000020004008080L, 0x0000800100020080L, 0x0000800041000080L,
            0x00FFFCDDFCED714AL, 0x007FFCDDFCED714AL, 0x003FFFCDFFD88096L, 0x0000040810002101L,
            0x0001000204080011L, 0x0001000204000801L, 0x0001000082000401L, 0x0001FFFAABFAD1A2L
    };

    /**
     * Precomputed magic numbers for bishops, used to hash occupancy patterns to indices in the bishop attack table.
     * Each entry corresponds to a square on the board.
     */
    public static final long[] BISHOP_MAGICS = new long[] {
            0x0002020202020200L, 0x0002020202020000L, 0x0004010202000000L, 0x0004040080000000L,
            0x0001104000000000L, 0x0000821040000000L, 0x0000410410400000L, 0x0000104104104000L,
            0x0000040404040400L, 0x0000020202020200L, 0x0000040102020000L, 0x0000040400800000L,
            0x0000011040000000L, 0x0000008210400000L, 0x0000004104104000L, 0x0000002082082000L,
            0x0004000808080800L, 0x0002000404040400L, 0x0001000202020200L, 0x0000800802004000L,
            0x0000800400A00000L, 0x0000200100884000L, 0x0000400082082000L, 0x0000200041041000L,
            0x0002080010101000L, 0x0001040008080800L, 0x0000208004010400L, 0x0000404004010200L,
            0x0000840000802000L, 0x0000404002011000L, 0x0000808001041000L, 0x0000404000820800L,
            0x0001041000202000L, 0x0000820800101000L, 0x0000104400080800L, 0x0000020080080080L,
            0x0000404040040100L, 0x0000808100020100L, 0x0001010100020800L, 0x0000808080010400L,
            0x0000820820004000L, 0x0000410410002000L, 0x0000082088001000L, 0x0000002011000800L,
            0x0000080100400400L, 0x0001010101000200L, 0x0002020202000400L, 0x0001010101000200L,
            0x0000410410400000L, 0x0000208208200000L, 0x0000002084100000L, 0x0000000020880000L,
            0x0000001002020000L, 0x0000040408020000L, 0x0004040404040000L, 0x0002020202020000L,
            0x0000104104104000L, 0x0000002082082000L, 0x0000000020841000L, 0x0000000000208800L,
            0x0000000010020200L, 0x0000000404080200L, 0x0000040404040400L, 0x0002020202020200L
    };

    /**
     * Precomputed shift values for rook magic bitboards. Each value indicates the number of bits to shift the hashed
     * occupancy pattern to obtain the index into the rook attack table for the corresponding square.
     */
    public static final int[] ROOK_SHIFTS = new int[]{
            52, 53, 53, 53, 53, 53, 53, 52,
            53, 54, 54, 54, 54, 54, 54, 53,
            53, 54, 54, 54, 54, 54, 54, 53,
            53, 54, 54, 54, 54, 54, 54, 53,
            53, 54, 54, 54, 54, 54, 54, 53,
            53, 54, 54, 54, 54, 54, 54, 53,
            53, 54, 54, 54, 54, 54, 54, 53,
            53, 54, 54, 53, 53, 53, 53, 53
    };

    /**
     * Precomputed shift values for bishop magic bitboards. Each value indicates the number of bits to shift the hashed
     * occupancy pattern to obtain the index into the bishop attack table for the corresponding square.
     */
    public static final int[] BISHOP_SHIFTS = new int[] {
            58, 59, 59, 59, 59, 59, 59, 58,
            59, 59, 59, 59, 59, 59, 59, 59,
            59, 59, 57, 57, 57, 57, 59, 59,
            59, 59, 57, 55, 55, 57, 59, 59,
            59, 59, 57, 55, 55, 57, 59, 59,
            59, 59, 57, 57, 57, 57, 59, 59,
            59, 59, 59, 59, 59, 59, 59, 59,
            58, 59, 59, 59, 59, 59, 59, 58
    };

    /**
     * Precomputed movement masks for rooks, representing the possible sliding destinations (excluding board edges)
     * for a rook on each square, used to determine relevant occupancy bits for magic bitboard calculations.
     */
    public static final long[] ROOK_MASKS = initMagicMask(true);

    /**
     * Precomputed movement masks for bishops, representing the possible sliding destinations (excluding board edges)
     * for a bishop on each square, used to determine relevant occupancy bits for magic bitboard calculations.
     */
    public static final long[] BISHOP_MASKS = initMagicMask(false);


    /**
     * Precomputed attack tables for rooks, mapping hashed occupancy patterns to valid attack bitboards for each square.
     */
    public static final long[][] ROOK_ATTACKS = initMagicAttacks(true, ROOK_MAGICS, ROOK_SHIFTS);

    /**
     * Precomputed attack tables for bishops, mapping hashed occupancy patterns to valid attack bitboards for each square.
     */
    public static final long[][] BISHOP_ATTACKS = initMagicAttacks(false, BISHOP_MAGICS, BISHOP_SHIFTS);

    /**
     * Lookup table for rook magic bitboards, containing precomputed attack sets, masks, magic numbers, and shifts
     * for each square to facilitate fast move generation.
     */
    public static final MagicLookup[] ROOK_MAGIC_LOOKUP = initMagicLookups(ROOK_ATTACKS, ROOK_MASKS, ROOK_MAGICS, ROOK_SHIFTS);

    /**
     * Lookup table for bishop magic bitboards, containing precomputed attack sets, masks, magic numbers, and shifts
     * for each square to facilitate fast move generation.
     */
    public static final MagicLookup[] BISHOP_MAGIC_LOOKUP = initMagicLookups(BISHOP_ATTACKS, BISHOP_MASKS, BISHOP_MAGICS, BISHOP_SHIFTS);

    /**
     * Calculates the attack bitboard for pawns of the specified color. For white pawns, attacks are diagonal moves
     * to the northwest and northeast; for black pawns, attacks are to the southwest and southeast. The method
     * accounts for board edges by excluding invalid moves (e.g., attacks wrapping from file A to H or vice versa).
     *
     * @param pawns   A bitboard representing the positions of pawns.
     * @param isWhite True if calculating for white pawns, false for black pawns.
     * @return A bitboard representing all possible attack destinations for the given pawns.
     */
    public static long pawnAttacks(long pawns, boolean isWhite) {
        return isWhite ?
                (BitwiseOperations.shiftNorthWest(pawns) &~ Bits.FILE_H) | (BitwiseOperations.shiftNorthEast(pawns) &~ Bits.FILE_A) :
                (BitwiseOperations.shiftSouthWest(pawns) &~ Bits.FILE_H) | (BitwiseOperations.shiftSouthEast(pawns) &~ Bits.FILE_A);
    }

    /**
     * Retrieves the precomputed attack bitboard for a king on the specified square.
     *
     * @param square The board square (0-63) where the king is located.
     * @return A bitboard representing all possible destinations for the king.
     */
    public static long kingAttacks(int square) {
        return KING_ATTACKS[square];
    }

    /**
     * Retrieves the precomputed attack bitboard for a knight on the specified square.
     *
     * @param square The board square (0-63) where the knight is located.
     * @return A bitboard representing all possible destinations for the knight.
     */
    public static long knightAttacks(int square) {
        return KNIGHT_ATTACKS[square];
    }

    /**
     * Calculates the attack bitboard for a rook on the specified square, considering blockers (occupied squares).
     * Uses magic bitboards to efficiently map occupancy to precomputed attack sets.
     *
     * @param square   The board square (0-63) where the rook is located.
     * @param blockers A bitboard representing occupied squares that may block the rook's movement.
     * @return A bitboard representing all possible attack destinations for the rook.
     */
    public static long rookAttacks(int square, long blockers) {
        return sliderAttacks(square, blockers, ROOK_MAGIC_LOOKUP);
    }

    /**
     * Calculates the attack bitboard for a bishop on the specified square, considering blockers (occupied squares).
     * Uses magic bitboards to efficiently map occupancy to precomputed attack sets.
     *
     * @param square   The board square (0-63) where the bishop is located.
     * @param blockers A bitboard representing occupied squares that may block the bishop's movement.
     * @return A bitboard representing all possible attack destinations for the bishop.
     */
    public static long bishopAttacks(int square, long blockers) {
        return sliderAttacks(square, blockers, BISHOP_MAGIC_LOOKUP);
    }

    /**
     * Generic method to calculate attack bitboards for sliding pieces (rooks or bishops) using magic bitboard lookups.
     * The occupancy is masked, multiplied by the magic number, and shifted to compute an index into the attack table.
     *
     * @param sq      The board square (0-63) where the piece is located.
     * @param occ     A bitboard representing occupied squares that may block the piece's movement.
     * @param lookups The magic lookup table for the piece type (rook or bishop).
     * @return A bitboard representing all possible attack destinations for the piece.
     */
    public static long sliderAttacks(int sq, long occ, MagicLookup[] lookups) {
        MagicLookup lookup = lookups[sq];
        occ      &= lookup.mask;
        occ      *= lookup.magic;
        occ    >>>= lookup.shift;
        return lookup.attacks[(int) occ];
    }

    /**
     * Initializes movement masks for rooks or bishops, representing the possible sliding destinations
     * (excluding board edges) for a piece on each square. These masks are used to determine relevant
     * occupancy bits for magic bitboard calculations.
     *
     * @param isOrthogonal True for rooks (orthogonal movement), false for bishops (diagonal movement).
     * @return An array of movement masks for each square.
     * */
    public static long[] initMagicMask(boolean isOrthogonal) {
        long[] magicMasks = new long[64];
        for (int square = 0; square < 64; square++) {
            magicMasks[square] = initMovementMask(square, isOrthogonal);
        }
        return magicMasks;
    }

    /**
     * Initializes attack tables for rooks or bishops, mapping hashed occupancy patterns to valid attack bitboards
     * for each square. Uses precomputed magic numbers and shifts for efficient lookup.
     *
     * @param isOrthogonal True for rooks, false for bishops.
     * @param magics       Array of magic numbers for the piece type.
     * @param shifts       Array of shift values for the piece type.
     * @return A 2D array of attack bitboards for each square and occupancy pattern.
     */
    public static long[][] initMagicAttacks(boolean isOrthogonal, long[] magics, int[] shifts) {
        long[][] magicAttacks = new long[64][];

        for (int square = 0; square < 64; square++) {
            magicAttacks[square] = initMagicTable(square, isOrthogonal, magics[square], shifts[square]);
        }

        return magicAttacks;
    }

    /**
     * Initializes the attack table for a specific square and piece type, mapping all possible blocker
     * configurations to their corresponding attack bitboards.
     *
     * @param square      The board square (0-63) for the piece.
     * @param isOrthogonal True for rooks, false for bishops.
     * @param magic       The magic number for hashing occupancy patterns.
     * @param shift       The number of bits to shift the hashed occupancy.
     * @return An array of attack bitboards for all possible blocker configurations.
     */
    public static long[] initMagicTable(int square, boolean isOrthogonal, long magic, int shift) {
        int numBits = 64 - shift;
        int tableSize = 1 << numBits;
        long[] table = new long[tableSize];

        long movementMask = initMovementMask(square, isOrthogonal);
        long[] blockerMasks = initBlockerMasks(movementMask);

        for (long blockerMask : blockerMasks) {
            long index = (blockerMask * magic) >>> shift;
            long attacks = initAttackMask(square, blockerMask, isOrthogonal);
            table[(int) index] = attacks;
        }
        return table;
    }

    /**
     * Generates all possible blocker configurations for a given movement mask, representing different
     * occupancy patterns that may block a sliding piece's movement.
     *
     * @param movementMask The movement mask for the piece on a specific square.
     * @return An array of bitboards representing all possible blocker configurations.
     */
    public static long[] initBlockerMasks(long movementMask) {
        List<Integer> moveSquares = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if ((movementMask & 1L << i) != 0) {
                moveSquares.add(i);
            }
        }

        int patternsCount = 1 << moveSquares.size();
        long[] blockerBitboards = new long[patternsCount];

        for (int patternIndex = 0; patternIndex < patternsCount; patternIndex++) {
            for (int bitIndex = 0; bitIndex < moveSquares.size(); bitIndex++) {
                int bit = (patternIndex >>> bitIndex) & 1;
                blockerBitboards[patternIndex] |= (long) bit << moveSquares.get(bitIndex);
            }
        }
        return blockerBitboards;
    }

    /**
     * Initializes the movement mask for a sliding piece on a specific square, representing the possible
     * destinations (excluding board edges) along orthogonal or diagonal vectors.
     *
     * @param startSquare  The board square (0-63) where the piece is located.
     * @param isOrthogonal True for rooks, false for bishops.
     * @return A bitboard representing the movement mask for the piece.
     */
    public static long initMovementMask(int startSquare, boolean isOrthogonal) {
        long movementMask = 0L;
        Set<Integer> vectors = isOrthogonal ? BoardUtils.ORTHOGONAL_MOVE_VECTORS : BoardUtils.DIAGONAL_MOVE_VECTORS;

        for (int vector : vectors) {
            int currentSquare = startSquare;
            if (!isValidVectorOffset(currentSquare, vector)) {
                continue;
            }
            for (int distance = 1; distance < 8; distance++) {
                currentSquare = currentSquare + vector;
                if (BoardUtils.isValidIndex(currentSquare + vector) && isValidVectorOffset(currentSquare, vector)) {
                    movementMask |= 1L << currentSquare;
                } else {
                    break;
                }
            }
        }
        return movementMask;
    }

    /**
     * Initializes the attack mask for a sliding piece on a specific square, considering blockers.
     * The mask represents all possible attack destinations along orthogonal or diagonal vectors,
     * stopping at the first blocker in each direction.
     *
     * @param startSquare  The board square (0-63) where the piece is located.
     * @param blockers     A bitboard representing occupied squares that may block the piece's movement.
     * @param isOrthogonal True for rooks, false for bishops.
     * @return A bitboard representing the attack destinations for the piece.
     */
    public static long initAttackMask(int startSquare, long blockers, boolean isOrthogonal) {

        long attackMask = 0L;
        Set<Integer> vectors = isOrthogonal ? BoardUtils.ORTHOGONAL_MOVE_VECTORS : BoardUtils.DIAGONAL_MOVE_VECTORS;

        for (int vector : vectors) {
            int currentSquare = startSquare;
            for (int distance = 1; distance < 8; distance++) {
                if (BoardUtils.isValidIndex(currentSquare + vector) && isValidVectorOffset(currentSquare, vector)) {
                    currentSquare = currentSquare + vector;
                    attackMask |= 1L << currentSquare;
                    if ((blockers & 1L << currentSquare) != 0) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return attackMask;

    }

    /**
     * Initializes the magic lookup table for a piece type, combining attack sets, movement masks,
     * magic numbers, and shifts for each square.
     *
     * @param allAttacks Array of attack tables for each square.
     * @param masks      Array of movement masks for each square.
     * @param magics     Array of magic numbers for each square.
     * @param shifts     Array of shift values for each square.
     * @return An array of MagicLookup records for fast move generation.
     */
    public static MagicLookup[] initMagicLookups(long[][] allAttacks, long[] masks, long[] magics, int[] shifts) {
        MagicLookup[] magicLookups = new MagicLookup[64];
        for (int i = 0; i < 64; i++) {
            long[] attacks = allAttacks[i];
            long mask = masks[i];
            long magic = magics[i];
            int shift = shifts[i];
            magicLookups[i] = new MagicLookup(attacks, mask, magic, shift);
        }
        return magicLookups;
    }

    /**
     * Checks if a vector offset is valid for a given square, accounting for board edge constraints
     * (e.g., preventing moves that wrap from file A to H or vice versa).
     *
     * @param square       The board square (0-63) to check.
     * @param vectorOffset The movement vector to validate.
     * @return True if the vector offset is valid for the square, false otherwise.
     */
    private static boolean isValidVectorOffset(int square, int vectorOffset) {
        boolean isAFile = (Bits.FILE_A & 1L << square) != 0;
        boolean isHFile = (Bits.FILE_H & 1L << square) != 0;
        boolean isVectorAFileException = BoardUtils.A_FILE_OFFSET_EXCEPTIONS.contains(vectorOffset);
        boolean isVectorHFileException = BoardUtils.H_FILE_OFFSET_EXCEPTIONS.contains(vectorOffset);
        return (!isAFile || !isVectorAFileException) && (!isHFile || !isVectorHFileException);
    }

    /**
     * Record representing a magic bitboard lookup structure, containing the attack table, movement mask,
     * magic number, and shift value for a specific square.
     *
     * @param attacks The precomputed attack bitboards for all possible occupancy patterns.
     * @param mask    The movement mask for the square.
     * @param magic   The magic number for hashing occupancy patterns.
     * @param shift   The number of bits to shift the hashed occupancy.
     * @author Utsab Dahal
     */
    public record MagicLookup(long[] attacks, long mask, long magic, int shift) {}

}
