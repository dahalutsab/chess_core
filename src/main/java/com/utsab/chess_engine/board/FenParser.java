package com.utsab.chess_engine.board;

import java.util.logging.Logger;

public class FenParser {
    private static final Logger logger = Logger.getLogger(FenParser.class.getName());

    public static Bitboard loadFromFen(String fen) {
        Bitboard bitboard = new Bitboard();
        String[] parts = fen.split(" ");
        String[] ranks = parts[0].split("/");

        bitboard.setWhiteToMove(parts[1].equals("w"));

        int square = 56; // Start at a8
        for (String rank : ranks) {
            for (char c : rank.toCharArray()) {
                if (Character.isDigit(c)) {
                    square += Character.getNumericValue(c);
                } else {
                    int pieceIndex = "PNBRQKpnbrqk".indexOf(c);
                    if (pieceIndex != -1) {
                        long currentBitboard = bitboard.getPieceBitboard(pieceIndex);
                        currentBitboard |= (1L << square);
                        bitboard.setPieceBitboard(pieceIndex, currentBitboard);

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
            square -= 16;
        }
        logger.info("Loaded FEN: " + fen + "\nBoard state:\n" + bitboard);
        return bitboard;
    }
}