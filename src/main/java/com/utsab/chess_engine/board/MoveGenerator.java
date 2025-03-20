package com.utsab.chess_engine.board;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    private final Bitboard bitboard;

    public MoveGenerator(Bitboard bitboard) {
        this.bitboard = bitboard;
    }

    public List<Move> generateMoves() {
        List<Move> moves = new ArrayList<>();

        // Generate moves for all pieces
        generatePawnMoves(moves);
        generateKnightMoves(moves);
        generateBishopMoves(moves);
        generateRookMoves(moves);
        generateQueenMoves(moves);
        generateKingMoves(moves);

        return moves;
    }

    private void generatePawnMoves(List<Move> moves) {
        long whitePawns = bitboard.getPieceBitboard(0); // White pawns
        while (whitePawns != 0) {
            int fromSquare = Long.numberOfTrailingZeros(whitePawns);
            int rank = fromSquare / 8;
            int file = fromSquare % 8;

            // Move forward one square
            int toSquare = fromSquare - 8;
            if (toSquare >= 0 && bitboard.isSquareEmpty(toSquare)) {
                moves.add(new Move(fromSquare, toSquare));

                // Move forward two squares (if on starting rank)
                if (rank == 6) {
                    toSquare = fromSquare - 16;
                    if (bitboard.isSquareEmpty(toSquare)) {
                        moves.add(new Move(fromSquare, toSquare));
                    }
                }
            }

            // Capture diagonally
            if (file > 0) {
                toSquare = fromSquare - 9;
                if (toSquare >= 0 && !bitboard.isSquareEmpty(toSquare)) {
                    moves.add(new Move(fromSquare, toSquare));
                }
            }
            if (file < 7) {
                toSquare = fromSquare - 7;
                if (toSquare >= 0 && !bitboard.isSquareEmpty(toSquare)) {
                    moves.add(new Move(fromSquare, toSquare));
                }
            }

            whitePawns &= whitePawns - 1; // Clear the least significant bit
        }
    }

    private void generateKnightMoves(List<Move> moves) {
        long whiteKnights = bitboard.getPieceBitboard(1); // White knights
        while (whiteKnights != 0) {
            int fromSquare = Long.numberOfTrailingZeros(whiteKnights);
            int[] knightMoves = {-17, -15, -10, -6, 6, 10, 15, 17};

            for (int offset : knightMoves) {
                int toSquare = fromSquare + offset;
                if (toSquare >= 0 && toSquare < 64 && bitboard.isSquareEmpty(toSquare)) {
                    int fromRank = fromSquare / 8;
                    int fromFile = fromSquare % 8;
                    int toRank = toSquare / 8;
                    int toFile = toSquare % 8;

                    // Ensure the move is within the board and follows the knight's L-shape
                    if (Math.abs(fromRank - toRank) + Math.abs(fromFile - toFile) == 3) {
                        moves.add(new Move(fromSquare, toSquare));
                    }
                }
            }

            whiteKnights &= whiteKnights - 1; // Clear the least significant bit
        }
    }

    private void generateBishopMoves(List<Move> moves) {
        long whiteBishops = bitboard.getPieceBitboard(2); // White bishops
        while (whiteBishops != 0) {
            int fromSquare = Long.numberOfTrailingZeros(whiteBishops);
            int[] bishopMoves = {-9, -7, 7, 9};

            for (int offset : bishopMoves) {
                int toSquare = fromSquare + offset;
                while (toSquare >= 0 && toSquare < 64) {
                    int fromRank = fromSquare / 8;
                    int fromFile = fromSquare % 8;
                    int toRank = toSquare / 8;
                    int toFile = toSquare % 8;

                    // Ensure the move is within the board and follows the bishop's diagonal movement
                    if (Math.abs(fromRank - toRank) == Math.abs(fromFile - toFile)) {
                        if (bitboard.isSquareEmpty(toSquare)) {
                            moves.add(new Move(fromSquare, toSquare));
                        } else {
                            moves.add(new Move(fromSquare, toSquare));
                            break;
                        }
                    }
                    toSquare += offset;
                }
            }

            whiteBishops &= whiteBishops - 1; // Clear the least significant bit
        }
    }

    private void generateRookMoves(List<Move> moves) {
        long whiteRooks = bitboard.getPieceBitboard(3); // White rooks
        while (whiteRooks != 0) {
            int fromSquare = Long.numberOfTrailingZeros(whiteRooks);
            int[] rookMoves = {-8, -1, 1, 8};

            for (int offset : rookMoves) {
                int toSquare = fromSquare + offset;
                while (toSquare >= 0 && toSquare < 64) {
                    int fromRank = fromSquare / 8;
                    int fromFile = fromSquare % 8;
                    int toRank = toSquare / 8;
                    int toFile = toSquare % 8;

                    // Ensure the move is within the board and follows the rook's straight-line movement
                    if (fromRank == toRank || fromFile == toFile) {
                        if (bitboard.isSquareEmpty(toSquare)) {
                            moves.add(new Move(fromSquare, toSquare));
                        } else {
                            moves.add(new Move(fromSquare, toSquare));
                            break;
                        }
                    }
                    toSquare += offset;
                }
            }

            whiteRooks &= whiteRooks - 1; // Clear the least significant bit
        }
    }

    private void generateQueenMoves(List<Move> moves) {
        long whiteQueens = bitboard.getPieceBitboard(4); // White queens
        while (whiteQueens != 0) {
            int fromSquare = Long.numberOfTrailingZeros(whiteQueens);
            int[] queenMoves = {-9, -8, -7, -1, 1, 7, 8, 9};

            for (int offset : queenMoves) {
                int toSquare = fromSquare + offset;
                while (toSquare >= 0 && toSquare < 64) {
                    int fromRank = fromSquare / 8;
                    int fromFile = fromSquare % 8;
                    int toRank = toSquare / 8;
                    int toFile = toSquare % 8;

                    // Ensure the move is within the board and follows the queen's combined movement
                    if (Math.abs(fromRank - toRank) == Math.abs(fromFile - toFile) || fromRank == toRank || fromFile == toFile) {
                        if (bitboard.isSquareEmpty(toSquare)) {
                            moves.add(new Move(fromSquare, toSquare));
                        } else {
                            moves.add(new Move(fromSquare, toSquare));
                            break;
                        }
                    }
                    toSquare += offset;
                }
            }

            whiteQueens &= whiteQueens - 1; // Clear the least significant bit
        }
    }

    private void generateKingMoves(List<Move> moves) {
        long whiteKing = bitboard.getPieceBitboard(5); // White king
        long blackKing = bitboard.getPieceBitboard(11); // Black king

        if (whiteKing != 0) {
            int fromSquare = Long.numberOfTrailingZeros(whiteKing);
            int[] kingMoves = {-9, -8, -7, -1, 1, 7, 8, 9};

            for (int offset : kingMoves) {
                int toSquare = fromSquare + offset;
                if (toSquare >= 0 && toSquare < 64) {
                    int fromRank = fromSquare / 8;
                    int fromFile = fromSquare % 8;
                    int toRank = toSquare / 8;
                    int toFile = toSquare % 8;

                    // Ensure the move is within the board
                    if (Math.abs(fromRank - toRank) <= 1 && Math.abs(fromFile - toFile) <= 1) {
                        // Check if the destination square is adjacent to the black king
                        int blackKingSquare = Long.numberOfTrailingZeros(blackKing);
                        int blackKingRank = blackKingSquare / 8;
                        int blackKingFile = blackKingSquare % 8;

                        if (Math.abs(toRank - blackKingRank) > 1 || Math.abs(toFile - blackKingFile) > 1) {
                            if (bitboard.isSquareEmpty(toSquare) || !bitboard.isSquareEmpty(toSquare)) {
                                moves.add(new Move(fromSquare, toSquare));
                            }
                        }
                    }
                }
            }
        }
    }

    public void logValidMoves() {
        List<Move> moves = generateMoves();
        System.out.println("Valid Moves:");
        for (Move move : moves) {
            System.out.println(move);
        }

        // Print valid moves on an ASCII board
        printValidMovesOnBoard(moves);
    }

    private void printValidMovesOnBoard(List<Move> moves) {
        char[][] board = new char[8][8];

        // Initialize the board with empty squares
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                board[rank][file] = '.';
            }
        }

        // Mark the valid moves on the board
        for (Move move : moves) {
            int toSquare = move.toSquare();
            int rank = toSquare / 8;
            int file = toSquare % 8;
            board[rank][file] = 'X'; // Mark valid moves with 'X'
        }

        // Print the board
        System.out.println("Valid Moves on Board:");
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                System.out.print(board[rank][file] + " ");
            }
            System.out.println();
        }
    }
}