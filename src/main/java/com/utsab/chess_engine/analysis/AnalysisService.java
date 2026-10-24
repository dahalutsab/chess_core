package com.utsab.chess_engine.analysis;

import com.utsab.chess_engine.core.UChessEngineUCI;
import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.Move;
import com.utsab.chess_engine.core.evaluation.Score;
import com.utsab.chess_engine.core.search.SearchResult;
import com.utsab.chess_engine.core.utils.notation.Notation;
import com.utsab.chess_engine.core.utils.notation.SAN;
import com.utsab.chess_engine.exceptions.custom.InvalidRequestException;
import com.utsab.chess_engine.game.entity.Game;
import com.utsab.chess_engine.game.repository.GameRepository;
import com.utsab.chess_engine.move.entity.MoveEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Reviews a finished game with the engine: evaluates every position, finds the best move in each, and grades the
 * moves that were played by how much winning chance they gave away.
 * <p>
 * Winning chances and accuracy use the formulas lichess publishes, so the numbers behave like those players know.
 *
 * @author Utsab Dahal
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    /** Search time per position. A game of 60 plies takes about six seconds. */
    private static final int THINK_MS_PER_POSITION = 100;
    private static final int ANALYSIS_DEPTH = 64;
    private static final int MAX_PLIES = 300;
    private static final int CACHE_SIZE = 100;

    /**
     * Engine evaluation of a position, from White's point of view: centipawns, or moves to a forced mate (negative when
     * Black mates). For a position where the game is over, {@code result} is "1-0", "0-1" or "1/2-1/2" instead.
     */
    public record PositionEval(int ply, Integer cp, Integer mate, String result, String bestMove, String bestSan) {}

    /**
     * @param classification best, good, inaccuracy, mistake or blunder
     * @param winChanceLoss  how many percentage points of winning chance the move gave away
     */
    public record MoveReview(int ply, String uci, String san, String classification, double winChanceLoss,
                             String bestMove, String bestSan) {}

    public record GameAnalysis(UUID gameId, List<PositionEval> positions, List<MoveReview> moves,
                               double whiteAccuracy, double blackAccuracy) {}

    private final GameRepository gameRepository;

    /** Finished games don't change, so their analysis is kept for repeat views */
    private final Map<UUID, GameAnalysis> cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, GameAnalysis> eldest) {
            return size() > CACHE_SIZE;
        }
    });

    public GameAnalysis analyse(UUID gameId) {
        GameAnalysis cached = cache.get(gameId);
        if (cached != null) return cached;

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new InvalidRequestException("Game not found: " + gameId));
        if (!game.getResult().isFinished()) {
            throw new InvalidRequestException("Games can be analysed once they are over");
        }
        List<String> uciMoves = game.getMoveEntities().stream().map(MoveEntity::getUci).toList();
        if (uciMoves.size() > MAX_PLIES) uciMoves = uciMoves.subList(0, MAX_PLIES);

        long started = System.currentTimeMillis();
        List<PositionEval> positions = new ArrayList<>();
        List<String> sans = new ArrayList<>();
        for (int ply = 0; ply <= uciMoves.size(); ply++) {
            // One position at a time, so games against bots aren't held up for the whole analysis
            synchronized (UChessEngineUCI.ENGINE) {
                positions.add(evaluate(game.getInitialFen(), uciMoves, ply, sans));
            }
        }

        List<MoveReview> reviews = new ArrayList<>();
        List<Double> whiteAccuracies = new ArrayList<>();
        List<Double> blackAccuracies = new ArrayList<>();
        for (int i = 0; i < uciMoves.size(); i++) {
            PositionEval before = positions.get(i);
            PositionEval after = positions.get(i + 1);
            boolean whiteMoved = isWhiteToMove(game.getInitialFen(), i);
            double loss = Math.max(0, moverWinChance(before, whiteMoved) - moverWinChance(after, whiteMoved));
            String uci = uciMoves.get(i);
            String classification = uci.equals(before.bestMove()) ? "best" : classify(loss);
            reviews.add(new MoveReview(i + 1, uci, sans.get(i), classification, round1(loss), before.bestMove(), before.bestSan()));
            (whiteMoved ? whiteAccuracies : blackAccuracies).add(moveAccuracy(loss));
        }

        GameAnalysis analysis = new GameAnalysis(gameId, positions, reviews,
                round1(average(whiteAccuracies)), round1(average(blackAccuracies)));
        cache.put(gameId, analysis);
        log.info("[AnalysisService] Analysed game {} ({} plies) in {} ms", gameId, uciMoves.size(),
                System.currentTimeMillis() - started);
        return analysis;
    }

    /** Evaluate the position after the first {@code ply} moves; records the SAN of the move played from it */
    private PositionEval evaluate(String fen, List<String> uciMoves, int ply, List<String> sans) {
        List<Move> moves = uciMoves.subList(0, ply).stream().map(Notation::fromCombinedNotation).toList();
        UChessEngineUCI.ENGINE.setPosition(fen, moves);
        Board board = UChessEngineUCI.ENGINE.getBoard();
        boolean whiteToMove = board.isWhiteToMove();
        List<Move> legal = UChessEngineUCI.ENGINE.getMoveGenerator().generateMoves(board);

        if (ply < uciMoves.size()) {
            String played = uciMoves.get(ply);
            sans.add(legal.stream().filter(m -> Notation.toNotation(m).equals(played)).findFirst()
                    .map(m -> SAN.toSan(m, board)).orElse(played));
        }

        if (legal.isEmpty()) {
            boolean mated = UChessEngineUCI.ENGINE.getMoveGenerator().isCheck(board, whiteToMove);
            String result = !mated ? "1/2-1/2" : whiteToMove ? "0-1" : "1-0";
            return new PositionEval(ply, null, null, result, null, null);
        }

        UChessEngineUCI.ENGINE.getConfig().setMaxDepth(ANALYSIS_DEPTH);
        SearchResult result = UChessEngineUCI.ENGINE.think(THINK_MS_PER_POSITION);
        String bestMove = result.move() != null ? Notation.toNotation(result.move()) : null;
        String bestSan = result.move() != null ? SAN.toSan(result.move(), board) : null;
        int whiteEval = whiteToMove ? result.eval() : -result.eval();
        if (Score.isMateScore(whiteEval)) {
            int plies = Score.MATE_SCORE - Math.abs(whiteEval);
            int movesToMate = Math.max(1, (plies + 1) / 2);
            return new PositionEval(ply, null, whiteEval > 0 ? movesToMate : -movesToMate, null, bestMove, bestSan);
        }
        return new PositionEval(ply, whiteEval, null, null, bestMove, bestSan);
    }

    private static boolean isWhiteToMove(String fen, int ply) {
        String[] fields = fen.split(" ");
        boolean whiteStarts = fields.length < 2 || !"b".equals(fields[1]);
        return (ply % 2 == 0) == whiteStarts;
    }

    /** White's winning chance in percent (lichess' model), with forced mates as certain */
    static double whiteWinChance(PositionEval eval) {
        if (eval.result() != null) return switch (eval.result()) {
            case "1-0" -> 100;
            case "0-1" -> 0;
            default -> 50;
        };
        if (eval.mate() != null) return eval.mate() > 0 ? 100 : 0;
        int cp = Math.max(-1000, Math.min(1000, eval.cp()));
        return 50 + 50 * (2 / (1 + Math.exp(-0.00368208 * cp)) - 1);
    }

    private static double moverWinChance(PositionEval eval, boolean white) {
        double whiteChance = whiteWinChance(eval);
        return white ? whiteChance : 100 - whiteChance;
    }

    static String classify(double winChanceLoss) {
        if (winChanceLoss >= 20) return "blunder";
        if (winChanceLoss >= 10) return "mistake";
        if (winChanceLoss >= 5) return "inaccuracy";
        return "good";
    }

    /** Accuracy of a single move from the winning chance it lost (lichess' formula), 0 to 100 */
    static double moveAccuracy(double winChanceLoss) {
        double accuracy = 103.1668 * Math.exp(-0.04354 * winChanceLoss) - 3.1669;
        return Math.max(0, Math.min(100, accuracy));
    }

    private static double average(List<Double> values) {
        return values.isEmpty() ? 0 : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
