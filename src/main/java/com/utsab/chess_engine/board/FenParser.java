package com.utsab.chess_engine.board;

import java.util.logging.Logger;

public class FenParser {
    private static final Logger logger = Logger.getLogger(FenParser.class.getName());

    public static Bitboard loadFromFen(String fen) {
        Bitboard bitboard = new Bitboard();
        String[] parts = fen.split(" ");
        String[] ranks = parts[0].split("/");

        // Set whose turn it is
        bitboard.setWhiteToMove(parts[1].equals("w"));

        // Parse piece placement
        int square = 56; // Start at a8 (top-left corner)
        for (String rank : ranks) {
            for (char c : rank.toCharArray()) {
                if (Character.isDigit(c)) {
                    square += Character.getNumericValue(c); // Skip empty squares
                } else {
                    int pieceIndex = "PNBRQKpnbrqk".indexOf(c);
                    if (pieceIndex != -1) {
                        // Get the current bitboard, modify it, and set it back
                        long currentBitboard = bitboard.getPieceBitboard(pieceIndex);
                        currentBitboard |= (1L << square);
                        bitboard.setPieceBitboard(pieceIndex, currentBitboard);

                        // Update occupied, whitePieces, and blackPieces
                        long currentOccupied = bitboard.getOccupied();
                        currentOccupied |= (1L << square);
                        bitboard.setOccupied(currentOccupied);

                        if (pieceIndex < 6) {
                            long currentWhite = bitboard.getWhitePieces();
                            currentWhite |= (1L << square);
                            bitboard.setWhitePieces(currentWhite);
                        } else {
                            long currentBlack = bitboard.getBlackPieces();
                            currentBlack |= (1L << square);
                            bitboard.setBlackPieces(currentBlack);
                        }
                        logger.info("Placed piece " + c + " at square " + square);
                    }
                    square++;
                }
            }
            square -= 16; // Move to the next rank (down one row)
        }
        logger.info("Board after FEN parsing:\n" + bitboard);
        return bitboard;
    }
}