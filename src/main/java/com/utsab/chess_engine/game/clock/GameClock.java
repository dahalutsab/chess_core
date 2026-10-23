package com.utsab.chess_engine.game.clock;

import com.utsab.chess_engine.core.board.BitOperations;
import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.game.dto.ClockDto;
import com.utsab.chess_engine.game.entity.Game;
import com.utsab.chess_engine.game.entity.enums.GameResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-side chess clock for timed games. The server is the authority on time: each side is charged for the time
 * between the opponent's move being recorded and its own move arriving, so a client cannot gain time by tampering with
 * its local clock display.
 * <p>
 * As on most chess sites, the clocks only start running once both sides have made their first move. Time-outs are
 * detected lazily: whenever a game is read or a move is submitted, a side whose time has run out loses on time (or
 * draws, if the opponent has no mating material).
 *
 * @author Utsab Dahal
 * @since 2025
 */
public final class GameClock {

    /** Value of {@link Game#getTermination()} for games decided by a time-out. */
    public static final String TIMEOUT = "TIMEOUT";

    /** Longest the bot thinks per move; this is also the think time for untimed games. */
    public static final int MAX_BOT_THINK_MS = 1000;
    private static final int MIN_BOT_THINK_MS = 20;

    /** "minutes" or "minutes|incrementSeconds", e.g. "5" or "15|10". */
    private static final Pattern TIME_CONTROL = Pattern.compile("^(\\d{1,3})(?:\\|(\\d{1,3}))?$");

    public record TimeControl(long baseMs, long incrementMs) {}

    private GameClock() {}

    /**
     * Parse a time control string such as "5", "3|2" or "15|10".
     *
     * @return the time control, or null for an untimed game
     */
    public static TimeControl parse(String timeControl) {
        if (timeControl == null || timeControl.isBlank()) return null;
        Matcher matcher = TIME_CONTROL.matcher(timeControl.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time control: " + timeControl);
        }
        long minutes = Long.parseLong(matcher.group(1));
        long incrementSeconds = matcher.group(2) != null ? Long.parseLong(matcher.group(2)) : 0;
        if (minutes == 0) return null;
        return new TimeControl(minutes * 60_000, incrementSeconds * 1000);
    }

    public static void start(Game game, TimeControl timeControl, long now) {
        if (timeControl == null) return;
        game.setBaseTimeMs(timeControl.baseMs());
        game.setIncrementMs(timeControl.incrementMs());
        game.setWhiteTimeMs(timeControl.baseMs());
        game.setBlackTimeMs(timeControl.baseMs());
        game.setClockStartedAt(now);
    }

    public static boolean isTimed(Game game) {
        return game.getBaseTimeMs() != null;
    }

    /** Whether the side to move's clock is currently running. */
    public static boolean isRunning(Game game) {
        return isTimed(game) && game.getResult() == GameResult.IN_PROGRESS && game.getMoveEntities().size() >= 2;
    }

    /** Time left for the given side right now, which may be zero or negative if it has run out. */
    public static long remaining(Game game, boolean white, boolean whiteToMove, long now) {
        long stored = white ? game.getWhiteTimeMs() : game.getBlackTimeMs();
        if (isRunning(game) && white == whiteToMove) {
            stored -= now - game.getClockStartedAt();
        }
        return stored;
    }

    /** Whether the side to move has run out of time. */
    public static boolean isFlagged(Game game, boolean whiteToMove, long now) {
        return isRunning(game) && remaining(game, whiteToMove, whiteToMove, now) <= 0;
    }

    /**
     * Charge the side that just moved for its thinking time and add the increment, then start the opponent's clock.
     * Must be called before the move is added to the game.
     *
     * @return false if the side ran out of time before moving, in which case the move must not be played
     */
    public static boolean recordMove(Game game, boolean white, long now) {
        if (!isTimed(game)) return true;
        if (isRunning(game)) {
            long left = remaining(game, white, white, now);
            if (left <= 0) {
                setTime(game, white, 0);
                return false;
            }
            setTime(game, white, left + game.getIncrementMs());
        }
        game.setClockStartedAt(now);
        return true;
    }

    /**
     * Record a time-out by the given side: it loses, unless its opponent cannot possibly checkmate, which is a draw.
     */
    public static void flag(Game game, Board board, boolean flaggedWhite) {
        setTime(game, flaggedWhite, 0);
        boolean opponentCanMate = hasMatingMaterial(board, !flaggedWhite);
        if (!opponentCanMate) {
            game.setResult(GameResult.DRAW_BY_INSUFFICIENT_MATERIAL);
        } else {
            game.setResult(flaggedWhite ? GameResult.BLACK_WIN : GameResult.WHITE_WIN);
        }
        game.setTermination(TIMEOUT);
    }

    /**
     * How long the bot should think: at most {@link #MAX_BOT_THINK_MS}, and less when its clock is low, so that it
     * budgets its time across the rest of the game instead of losing on time.
     */
    public static int botThinkTime(Game game, boolean botIsWhite, long now) {
        if (!isRunning(game)) return MAX_BOT_THINK_MS;
        long left = remaining(game, botIsWhite, botIsWhite, now);
        long budget = Math.min(left / 30 + game.getIncrementMs() * 3 / 4, left / 2);
        return (int) Math.max(MIN_BOT_THINK_MS, Math.min(MAX_BOT_THINK_MS, budget));
    }

    /** The clock state to send to clients, or null for an untimed game. */
    public static ClockDto snapshot(Game game, boolean whiteToMove, long now) {
        if (!isTimed(game)) return null;
        return new ClockDto(
                game.getBaseTimeMs(),
                game.getIncrementMs(),
                Math.max(0, remaining(game, true, whiteToMove, now)),
                Math.max(0, remaining(game, false, whiteToMove, now)),
                isRunning(game)
        );
    }

    private static boolean hasMatingMaterial(Board board, boolean white) {
        long heavyOrPawns = board.getPawns(white) | board.getRooks(white) | board.getQueens(white);
        int minors = BitOperations.countBits(board.getKnights(white) | board.getBishops(white));
        return heavyOrPawns != 0 || minors >= 2;
    }

    private static void setTime(Game game, boolean white, long timeMs) {
        if (white) game.setWhiteTimeMs(timeMs);
        else game.setBlackTimeMs(timeMs);
    }
}
