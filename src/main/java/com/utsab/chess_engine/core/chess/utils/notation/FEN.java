package com.utsab.chess_engine.core.chess.utils.notation;

import com.utsab.chess_engine.core.chess.board.Board;
import com.utsab.chess_engine.core.chess.board.Zobrist;
import com.utsab.chess_engine.core.chess.utils.BoardUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A utility class for parsing Forsyth-Edwards Notation (FEN), a standard notation for representing
 * chess positions. FEN encodes the board state, active color, castling availability, en passant target,
 * halfmove clock, and fullmove number. This class provides methods to convert a FEN string into a
 * {@link Board} object, maintaining compatibility with UCI chess engine requirements.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 * @see <a href="https://www.chessprogramming.org/Forsyth-Edwards_Notation">Chess Programming Wiki - FEN</a>
 */
public class FEN {

    /**
     * The FEN string representing the standard starting position of a chess game.
     */
    public static final String STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /**
     * Converts a FEN string into a {@link Board} object, initializing all board state components
     * including piece bitboards, side to move, castling rights, en passant target, and move counters.
     *
     * <p>The method parses the FEN string, which consists of six fields separated by spaces:
     * <ul>
     *     <li>Piece placement (ranks 8 to 1, files a to h)</li>
     *     <li>Side to move ("w" or "b")</li>
     *     <li>Castling availability (e.g., "KQkq" or "-")</li>
     *     <li>En passant target square (e.g., "e3" or "-")</li>
     *     <li>Halfmove clock (number of halfmoves since last capture or pawn move)</li>
     *     <li>Fullmove number (incremented after Black's move)</li>
     * </ul>
     *
     * @param fen the FEN string to parse (e.g., "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
     * @return a new {@link Board} instance representing the parsed position
     * @throws IllegalArgumentException if the FEN string is malformed or invalid
     */
    public static Board toBoard(String fen) {
        try {
            // Split FEN string into its components
            String[] parts = fen.split(" ");
            if (parts.length < 4) {
                throw new IllegalArgumentException("Invalid FEN: insufficient parts");
            }
            String[] files = parts[0].split("/");
            if (files.length != 8) {
                throw new IllegalArgumentException("Invalid FEN: must have 8 ranks");
            }

            // Initialize bitboards for all piece types
            long whitePawns = 0L;
            long whiteKnights = 0L;
            long whiteBishops = 0L;
            long whiteRooks = 0L;
            long whiteQueens = 0L;
            long whiteKing = 0L;
            long blackPawns = 0L;
            long blackKnights = 0L;
            long blackBishops = 0L;
            long blackRooks = 0L;
            long blackQueens = 0L;
            long blackKing = 0L;

            // Parse piece placement into a list of ranks, reversing to match board orientation
            List<List<String>> rankFileHash = Arrays.stream(files)
                    .map(file -> Arrays.stream(file.split(""))
                            .flatMap(FEN::parseSquare)
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
            Collections.reverse(rankFileHash);

            // Process each rank and file to set piece bitboards
            for (int rankIndex = 0; rankIndex < rankFileHash.size(); rankIndex++) {
                List<String> rank = rankFileHash.get(rankIndex);
                if (rank.size() != 8) {
                    throw new IllegalArgumentException("Invalid FEN: rank " + (8 - rankIndex) + " does not have 8 files");
                }
                for (int fileIndex = 0; fileIndex < rank.size(); fileIndex++) {
                    int squareIndex = BoardUtils.squareIndex(rankIndex, fileIndex);
                    String squareValue = rank.get(fileIndex);
                    long squareBB = 1L << squareIndex;
                    switch (squareValue) {
                        case "P" -> whitePawns |= squareBB;
                        case "N" -> whiteKnights |= squareBB;
                        case "B" -> whiteBishops |= squareBB;
                        case "R" -> whiteRooks |= squareBB;
                        case "Q" -> whiteQueens |= squareBB;
                        case "K" -> whiteKing |= squareBB;
                        case "p" -> blackPawns |= squareBB;
                        case "n" -> blackKnights |= squareBB;
                        case "b" -> blackBishops |= squareBB;
                        case "r" -> blackRooks |= squareBB;
                        case "q" -> blackQueens |= squareBB;
                        case "k" -> blackKing |= squareBB;
                    }
                }
            }

            // Parse additional FEN fields
            boolean isWhiteToMove = parseSideToMove(parts[1]);
            int castlingRights = parseCastlingRights(parts[2]);
            int enPassantFile = parseEnPassantFile(parts[3]);
            int fiftyMoveCounter = parts.length > 4 ? parseFiftyMoveCounter(parts[4]) : 0;

            // Initialize and configure the Board object
            Board board = new Board();
            board.setWhitePawns(whitePawns);
            board.setWhiteKnights(whiteKnights);
            board.setWhiteBishops(whiteBishops);
            board.setWhiteRooks(whiteRooks);
            board.setWhiteQueens(whiteQueens);
            board.setWhiteKing(whiteKing);
            board.setBlackPawns(blackPawns);
            board.setBlackKnights(blackKnights);
            board.setBlackBishops(blackBishops);
            board.setBlackRooks(blackRooks);
            board.setBlackQueens(blackQueens);
            board.setBlackKing(blackKing);
            board.recalculatePieces();
            board.setPieceList(BoardUtils.calculatePieceList(board));
            board.setWhiteToMove(isWhiteToMove);
            board.getGameState().setCastlingRights(castlingRights);
            board.getGameState().setEnPassantFile(enPassantFile);
            board.getGameState().setFiftyMoveCounter(fiftyMoveCounter);
            board.getGameState().setZobristKey(Zobrist.generateKey(board));
            board.getGameState().setPawnKey(Zobrist.generatePawnKey(board));

            return board;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid FEN string: " + fen, e);
        }
    }

    /**
     * Parses the side-to-move field from the FEN string.
     *
     * @param sideToMove the side-to-move indicator ("w" or "b")
     * @return {@code true} if White is to move, {@code false} if Black
     * @throws IllegalArgumentException if the side-to-move indicator is invalid
     */
    private static boolean parseSideToMove(String sideToMove) {
        return switch (sideToMove) {
            case "w" -> true;
            case "b" -> false;
            default -> throw new IllegalArgumentException("Invalid side to move: " + sideToMove);
        };
    }

    /**
     * Parses the castling availability field from the FEN string into a bitmask.
     *
     * <p>The bitmask uses the following structure:
     * <ul>
     *     <li>Bit 0: White kingside castling (K)</li>
     *     <li>Bit 1: White queenside castling (Q)</li>
     *     <li>Bit 2: Black kingside castling (k)</li>
     *     <li>Bit 3: Black queenside castling (q)</li>
     * </ul>
     *
     * @param castlingRights the castling availability string (e.g., "KQkq" or "-")
     * @return an integer bitmask representing castling rights
     */
    private static int parseCastlingRights(String castlingRights) {
        int castlingRightsMask = 0b0000;
        if (castlingRights.contains("K")) castlingRightsMask |= 0b0001;
        if (castlingRights.contains("Q")) castlingRightsMask |= 0b0010;
        if (castlingRights.contains("k")) castlingRightsMask |= 0b0100;
        if (castlingRights.contains("q")) castlingRightsMask |= 0b1000;
        return castlingRightsMask;
    }

    /**
     * Parses the en passant target square from the FEN string.
     *
     * @param enPassantFile the en passant target square (e.g., "e3" or "-")
     * @return the file index (0-7) of the en passant target, or -1 if none
     */
    private static int parseEnPassantFile(String enPassantFile) {
        if (enPassantFile.equals("-")) {
            return -1;
        }
        int square = Notation.fromNotation(enPassantFile);
        return BoardUtils.getFile(square);
    }

    /**
     * Parses the halfmove clock (fifty-move counter) from the FEN string.
     *
     * @param fiftyMoveCounter the halfmove clock string
     * @return the integer value of the halfmove clock
     * @throws NumberFormatException if the counter is not a valid integer
     */
    private static int parseFiftyMoveCounter(String fiftyMoveCounter) {
        try {
            return Integer.parseInt(fiftyMoveCounter);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid fifty-move counter: " + fiftyMoveCounter);
        }
    }

    /**
     * Parses a single square character from the piece placement field into a stream of piece or empty square indicators.
     *
     * <p>Letters (e.g., "P", "r") represent pieces, while digits (1-8) indicate consecutive empty squares.
     * Empty squares are represented as "x" in the output stream.
     *
     * @param square the character to parse (e.g., "P" or "2")
     * @return a stream of piece codes or "x" for empty squares
     * @throws IllegalArgumentException if the character is invalid
     */
    private static Stream<String> parseSquare(String square) {
        if (square.length() != 1) {
            throw new IllegalArgumentException("Invalid square character: " + square);
        }
        return switch (square.charAt(0)) {
            case '1', '2', '3', '4', '5', '6', '7', '8' ->
                    IntStream.range(0, Integer.parseInt(square)).mapToObj(i -> "x");
            case 'P', 'N', 'B', 'R', 'Q', 'K', 'p', 'n', 'b', 'r', 'q', 'k' -> Stream.of(square);
            default -> throw new IllegalArgumentException("Invalid square character: " + square);
        };
    }

    /**
     * Converts a {@link Board} object into a FEN string representation.
     *
     * <p>This method generates a FEN string that encodes the current board state, including piece placement,
     * active color, castling rights, en passant target square, halfmove clock, and fullmove number.
     *
     * @param board the {@link Board} object to convert to FEN
     * @return a FEN string representing the board state
     * @throws IllegalArgumentException if the board is null or invalid
     */
    public static String toFen(Board board) {
        if (board == null) {
            throw new IllegalArgumentException("Board cannot be null");
        }
        if (board.getGameState() == null) {
            throw new IllegalStateException("GameState is null");
        }

        // Piece placement
        StringBuilder piecePlacement = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                var piece = board.getPieceList()[square];
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        piecePlacement.append(emptyCount);
                        emptyCount = 0;
                    }
                    char[] fenSymbolArray = piece.getFenSymbol(board.getWhitePawns(), board.getBlackPawns(), square);
                    String fenSymbol = String.valueOf(fenSymbolArray);
                    if (!fenSymbol.matches("[PNBRQKpnbrqk]")) {
                        throw new IllegalStateException("Invalid FEN symbol: " + fenSymbol + " at square " + square);
                    }
                    piecePlacement.append(fenSymbol);
                }
            }
            if (emptyCount > 0) {
                piecePlacement.append(emptyCount);
            }
            if (rank > 0) {
                piecePlacement.append('/');
            }
        }

        // Active color
        String activeColor = board.isWhiteToMove() ? "w" : "b";

        // Castling rights
        StringBuilder castling = new StringBuilder();
        int rights = board.getGameState().getCastlingRights();
        if ((rights & 0b0001) != 0) castling.append('K');
        if ((rights & 0b0010) != 0) castling.append('Q');
        if ((rights & 0b0100) != 0) castling.append('k');
        if ((rights & 0b1000) != 0) castling.append('q');
        if (castling.isEmpty()) castling.append('-');

        // En passant target
        int epFile = board.getGameState().getEnPassantFile();
        String enPassant = "-";
        if (epFile >= 0 && epFile < 8) {
            int epRank = board.isWhiteToMove() ? 2 : 5; // Correct rank for en passant target
            enPassant = "" + (char)('a' + epFile) + (epRank + 1);
        }

        // Halfmove clock and fullmove number
        int halfmoveClock = board.getGameState().getFiftyMoveCounter();
        int fullmoveNumber = 1 + board.getMoveHistory().size() / 2;

        return String.format("%s %s %s %s %d %d",
                piecePlacement, activeColor, castling, enPassant, halfmoveClock, fullmoveNumber);
    }
}