package com.utsab.chess_engine.core.search;

import com.utsab.chess_engine.core.board.BitOperations;
import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.board.Move;
import com.utsab.chess_engine.core.board.Piece;
import com.utsab.chess_engine.core.engine.EngineConfig;
import com.utsab.chess_engine.core.evaluation.Evaluation;
import com.utsab.chess_engine.core.evaluation.Result;
import com.utsab.chess_engine.core.evaluation.Score;
import com.utsab.chess_engine.core.generation.MagicBitboards;
import com.utsab.chess_engine.core.generation.MoveGeneration;
import com.utsab.chess_engine.core.generation.MoveGeneration.MoveFilter;
import com.utsab.chess_engine.core.generation.check.RayCalculator;
import com.utsab.chess_engine.core.search.moveordering.MoveOrderer;
import com.utsab.chess_engine.core.search.moveordering.MoveOrdering;
import com.utsab.chess_engine.core.search.moveordering.StaticExchangeEvaluator;
import com.utsab.chess_engine.core.transposition.HashEntry;
import com.utsab.chess_engine.core.transposition.HashFlag;
import com.utsab.chess_engine.core.transposition.TranspositionTable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Iterative deepening is a search strategy that does a full search at a depth of 1 ply, then a full search at 2 ply,
 * then 3 ply and so on, until the time limit is exhausted. In case the timeout is reached in the middle of an iteration,
 * the search can still fall back on the best move found in the previous iteration. By prioritising searching the best
 * move found in the previous iteration, as well as the other ordering heuristics in the {@link MoveOrderer} -- and by
 * using a {@link TranspositionTable} -- the iterative approach is much more efficient than it might sound.
 *
 * @see <a href="https://www.chessprogramming.org/Iterative_Deepening">Chess Programming Wiki</a>
 */
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Searcher implements Search {

    EngineConfig config;
    ThreadManager threadManager;
    MoveGeneration moveGenerator;
    MoveOrdering moveOrderer;
    Evaluation evaluator;
    StaticExchangeEvaluator see;
    RayCalculator rayCalculator = new RayCalculator();
    TranspositionTable transpositionTable;

    /**
     * How many calls to {@link #isCancelled()} between reads of the system clock. Reading the clock at every node is
     * surprisingly expensive, and checking every 1024 nodes still reacts to the deadline within a millisecond.
     */
    static final int TIME_CHECK_INTERVAL = 1024;

    static final int MAX_PLY = 512;

    /**
     * Size of the per-thread evaluation cache. Iterative deepening revisits the same leaf positions on every iteration,
     * and the quiescence search results are not stored in the transposition table, so caching static evaluations saves
     * a lot of repeated work. 2^18 entries is 2 MB, small enough to stay mostly in the CPU cache.
     */
    static final int EVAL_CACHE_SIZE = 1 << 18;
    static final long EVAL_CACHE_KEY_MASK = 0xFFFFFFFFFF000000L;
    static final long EVAL_CACHE_SCORE_MASK = 0x0000000000FFFFFFL;
    static final int NO_EVAL = Integer.MIN_VALUE;
    static final int RAZOR_MARGIN = 250;

    Board board;
    int nodes;
    long startNanos;
    long deadlineNanos;
    int timeCheckCounter;
    boolean cancelled;

    /**
     * Shared between all threads of a {@link LazySMPSearcher}, so that helper threads stop as soon as the main thread
     * has finished. Null when searching single-threaded.
     */
    AtomicBoolean stopSignal;

    /**
     * Index of this thread within a {@link LazySMPSearcher}; 0 is the main thread. Helper threads start at staggered
     * depths so that they explore different parts of the tree instead of duplicating the main thread's work.
     */
    int threadIndex;

    int currentDepth;

    /**
     * Static evaluation at each ply of the current search line, used to tell whether the side to move is 'improving'
     * compared to its previous move.
     */
    final int[] staticEvals = new int[MAX_PLY];

    /**
     * Each entry packs the upper 40 bits of the Zobrist key with a 24-bit signed evaluation.
     */
    final long[] evalCache = new long[EVAL_CACHE_SIZE];

    Move bestMove;
    Move bestMoveCurrentDepth;
    int bestEval;
    int bestEvalCurrentDepth;

    public Searcher(EngineConfig config,
                    ThreadManager threadManager,
                    MoveGeneration moveGenerator,
                    MoveOrdering moveOrderer,
                    Evaluation evaluator,
                    TranspositionTable transpositionTable) {
        this.config = config;
        this.threadManager = threadManager;
        this.moveGenerator = moveGenerator;
        this.moveOrderer = moveOrderer;
        this.evaluator = evaluator;
        this.transpositionTable = transpositionTable;
        this.see = new StaticExchangeEvaluator();
    }

    /**
     * Search for the best move in the current {@link Board} position.
     * @param duration How long we should search
     * @return A {@link SearchResult} containing the best move and the current evaluation.
     */
    @Override
    public SearchResult negamax(Duration duration) {

        startNanos = System.nanoTime();
        deadlineNanos = startNanos + duration.toNanos();
        timeCheckCounter = 0;
        nodes = 0;
        currentDepth = 1 + (threadIndex % 2);
        bestMove = null;
        bestMoveCurrentDepth = null;
        bestEval = 0;
        bestEvalCurrentDepth = 0;
        cancelled = false;

        int alpha = Integer.MIN_VALUE + 1;
        int beta = Integer.MAX_VALUE - 1;
        int retryMultiplier = 0;
        SearchResult result = null;

        while (!isCancelled() && currentDepth < config.getMaxDepth()) {
            // Reset variables for the current depth iteration
            bestMoveCurrentDepth = null;
            bestEvalCurrentDepth = 0;

            // Perform alpha-beta search for the current depth
            int eval = search(currentDepth, 0, alpha, beta, true);

            // Update the best move and evaluation if a better move is found
            if (bestMoveCurrentDepth != null) {
                bestMove = bestMoveCurrentDepth;
                bestEval = bestEvalCurrentDepth;
                result = buildResult();
                threadManager.handleSearchResult(result);
            }

            // Check if search is cancelled or a checkmate is found
            if (isCancelled() || isCheckmateFoundAtCurrentDepth(currentDepth)) {
                break;
            }

            // Adjust the aspiration window in case the score fell outside the current window
            if (eval <= alpha) {
                // If score <= alpha, re-search with an expanded aspiration window
                retryMultiplier++;
                alpha -= config.getAspFailMargin() * retryMultiplier;
                beta += config.getAspMargin();
                continue;
            }
            if (eval >= beta) {
                // If score >= beta, re-search with an expanded aspiration window
                retryMultiplier++;
                beta += config.getAspFailMargin() * retryMultiplier;
                alpha -= config.getAspMargin();
                continue;
            }

            alpha = eval - config.getAspMargin();
            beta = eval + config.getAspMargin();

            // Increment depth and retry multiplier for next iteration
            retryMultiplier = 0;
            currentDepth++;
        }

        // If no move is found within the time limit, choose the first available move
        if (result == null && threadIndex > 0) {
            // A helper that completed no iteration has nothing useful to contribute; it must not be mistaken for a
            // deeper result than the main thread's, so report no move at all.
            moveOrderer.clear();
            return new SearchResult(0, null, 0, 0, nodes, 0);
        }
        if (result == null) {
            System.out.println("Time expired before a move was found!");
            List<Move> legalMoves = moveGenerator.generateMoves(board);
            bestMove = legalMoves.isEmpty() ? null : legalMoves.getFirst();
            result = buildResult();
        }

        // Clear move ordering cache and return the search result
        moveOrderer.clear();
        return result;

    }

    /**
     * Run a single iteration of the iterative deepening search for a specific depth.
     *
     * @param depth               The number of ply deeper left to go in the current search ('ply remaining').
     * @param ply                 The number of ply already examined in the current search ('ply from root').
     * @param alpha               The lower bound for child nodes at the current search depth.
     * @param beta                The upper bound for child nodes at the current search depth.
     * @param allowNull           Whether to allow null-move pruning in this search iteration.
     */
    public int search(int depth, int ply, int alpha, int beta, boolean allowNull) {

        // If timeout is reached, exit immediately
        if (isCancelled()) return alpha;

        // If depth is reached, drop into quiescence search
        if (depth <= 0) return quiescenceSearch(alpha, beta, 1, ply);

        // If the game is drawn by repetition, insufficient material or fifty move rule, return zero
        if (ply > 0 && isDraw()) return Score.DRAW_SCORE;

        // Exit early if we have already found a forced mate at an earlier ply
        // See https://www.chessprogramming.org/Mate_Distance_Pruning
        alpha = Math.max(alpha, -Score.MATE_SCORE + ply);
        beta = Math.min(beta, Score.MATE_SCORE - ply);
        if (alpha >= beta) return alpha;

        // Probe the transposition table in case this node has been searched before
        HashEntry transposition = transpositionTable.get(getKey(), ply);
        Move transpositionMove = transposition != null ? transposition.getMove() : null;
        if (isUsefulTransposition(transposition, depth, alpha, beta)) {
            if (ply == 0 && transpositionMove != null) {
                bestMoveCurrentDepth = transpositionMove;
                bestEvalCurrentDepth = transposition.getScore();
            }
            return transposition.getScore();
        }
        Move previousBestMove = ply == 0 ? bestMove : null;
        if (transpositionMove != null) {
            previousBestMove = transpositionMove;
        }

        // If the position has not been searched yet, the search will be potentially expensive. So let's search with a
        // reduced depth expecting to record a move that we can use later for a full-depth search.
        // See https://www.chessprogramming.org/Internal_Iterative_Deepening
        if (transposition == null && ply > 0 && depth >= config.getIirDepth()) {
            --depth;
        }

        boolean isInCheck = moveGenerator.isCheck(board, board.isWhiteToMove());

        // The static evaluation is only used by pruning techniques that are disabled when in check, so skip it then.
        int staticEval = isInCheck ? 0 : evaluate();
        if (!isInCheck && transposition != null) {
            // A stored search score is a more accurate estimate than the static evaluation, when its bound allows it
            int transpositionScore = transposition.getScore();
            HashFlag transpositionFlag = transposition.getFlag();
            if (!Score.isMateScore(transpositionScore)
                && (transpositionFlag == HashFlag.EXACT
                    || (transpositionFlag == HashFlag.LOWER && transpositionScore > staticEval)
                    || (transpositionFlag == HashFlag.UPPER && transpositionScore < staticEval))) {
                staticEval = transpositionScore;
            }
        }

        boolean pvNode = beta - alpha > 1;

        // If our position is better than it was two plies ago, late moves are more likely to matter, so prune and reduce
        // them less. See https://www.chessprogramming.org/Improving
        if (ply < MAX_PLY) staticEvals[ply] = isInCheck ? NO_EVAL : staticEval;
        boolean improving = !isInCheck && ply >= 2 && ply < MAX_PLY
                && staticEvals[ply - 2] != NO_EVAL && staticEval > staticEvals[ply - 2];

        // These pruning techniques run before move generation, so that nodes they prune don't pay for generating
        // moves that are never searched.
        if (!pvNode && !isInCheck && ply > 0) {
            // If the static evaluation + some significant margin is still above beta, then let's assume this position
            // is a cut-node and will fail-high, and not search any further.
            // See https://www.chessprogramming.org/Reverse_Futility_Pruning
            boolean isMateHunting = Score.isMateScore(alpha);
            if (depth <= config.getRfpDepth()
                && staticEval - config.getRfpMargin()[depth] > beta
                && !isMateHunting) {
                return staticEval - config.getRfpMargin()[depth];
            }

            // If the static evaluation + some significant margin is still above beta after giving the opponent two moveEntities
            // in a row (making a 'null' move), then let's assume this position is a cut-node and will fail-high, and
            // not search any further. Not attempted without pieces, where zugzwang makes passing unsound.
            // See https://www.chessprogramming.org/Null_Move_Pruning
            if (allowNull
                && depth >= config.getNmpDepth()
                && staticEval >= beta - config.getNmpMargin()
                && board.hasPiecesRemaining(board.isWhiteToMove())) {
                int reduction = 3 + depth / 4;
                board.makeNullMove();
                int eval = -search(depth - 1 - reduction, ply + 1, -beta, -beta + 1, false);
                board.unmakeNullMove();
                if (isCancelled()) return alpha;
                if (eval >= beta) {
                    // Don't trust unproven mate scores from a null-move search
                    int score = Score.isMateScore(eval) ? beta : eval;
                    transpositionTable.put(getKey(), HashFlag.LOWER, depth, ply, previousBestMove, score);
                    return score;
                }
            }
        }

        List<Move> moves = moveGenerator.generateMoves(board);

        if (moves.isEmpty()) {
            // If there are no legal moveEntities, and it's check, then it's checkmate. Otherwise, it's stalemate.
            return isInCheck ? -Score.MATE_SCORE + ply : Score.DRAW_SCORE;
        }

        if (ply == 0 && moves.size() == 1) {
            // If there is only one legal move at the root node, play that move immediately.
            int eval = isDraw() ? Score.DRAW_SCORE : evaluate();
            bestMoveCurrentDepth = moves.getFirst();
            bestEvalCurrentDepth = eval;
            cancelled = true;
            return eval;
        }

        // Give each move a 'score' based on how interesting or promising it looks. These scores will be used to select
        // the order in which moveEntities are evaluated.
        int[] scores = new int[moves.size()];
        for (int i = 0; i < moves.size(); i++) {
            scores[i] = moveOrderer.scoreMove(board, moves.get(i), previousBestMove, true, ply);
        }

        Move bestMove = null;
        HashFlag flag = HashFlag.UPPER;

        // Quiet moves may be pruned at this node by futility pruning or late move pruning (below). Those only apply to
        // moves that don't give check; rather than making each move to find out, precompute the squares from which
        // each piece type would check the opponent king, so most quiet moves can be pruned without touching the board.
        boolean isFutileNode = !pvNode && !isInCheck && depth <= config.getFpDepth() && staticEval + config.getFpMargin()[depth] < alpha;
        boolean isLmpNode = !pvNode && !isInCheck && depth <= config.getLmpDepth();
        int lmpMoveCount = depth * config.getLmpMultiplier();
        long pawnCheckSquares = 0, knightCheckSquares = 0, bishopCheckSquares = 0, rookCheckSquares = 0, discoveredCheckers = 0;
        if (isFutileNode || isLmpNode) {
            boolean isWhite = board.isWhiteToMove();
            int opponentKing = BitOperations.getNextBit(board.getKing(!isWhite));
            long occupied = board.getOccupied();
            pawnCheckSquares = MagicBitboards.pawnAttacks(1L << opponentKing, !isWhite);
            knightCheckSquares = MagicBitboards.knightAttacks(opponentKing);
            bishopCheckSquares = MagicBitboards.bishopAttacks(opponentKing, occupied);
            rookCheckSquares = MagicBitboards.rookAttacks(opponentKing, occupied);
            discoveredCheckers = calculateDiscoveredCheckers(board, isWhite, opponentKing, occupied);
        }

        for (int i = 0; i < moves.size(); i++) {

            // Incremental move ordering: it's unnecessary to sort the entire move list, since most of them are never evaluated.
            // So just select the move with the best score that hasn't been tried yet
            Move move = selectNextMove(moves, scores, i);
            boolean isCapture = board.pieceAt(move.getEndSquare()) != null || move.isEnPassant();
            boolean isPromotion = move.getPromotionPieceType() != null;

            if (!isCapture && !isPromotion && (isFutileNode || (isLmpNode && i >= lmpMoveCount))) {
                // Same conditions as the futility and late move pruning below, applied before making the move
                long checkSquares = switch (board.pieceAt(move.getStartSquare())) {
                    case PAWN -> pawnCheckSquares;
                    case KNIGHT -> knightCheckSquares;
                    case BISHOP -> bishopCheckSquares;
                    case ROOK -> rookCheckSquares;
                    case QUEEN -> bishopCheckSquares | rookCheckSquares;
                    case KING -> 0L;
                };
                boolean mayGiveCheck = move.isCastling()
                        || (checkSquares & (1L << move.getEndSquare())) != 0
                        || (discoveredCheckers & (1L << move.getStartSquare())) != 0;
                if (!mayGiveCheck) {
                    continue;
                }
            }

            board.makeMove(move);
            nodes++;

            boolean isCheck = moveGenerator.isCheck(board, board.isWhiteToMove());
            boolean isQuiet = !isCheck && !isCapture && !isPromotion;

            // Futility pruning: if the static evaluation + some margin is still < alpha, and the current move is not
            // interesting (checks, captures, promotions), then let's assume it will fail low and prune this node.
            // See https://www.chessprogramming.org/Futility_Pruning
            if (!pvNode && depth <= config.getFpDepth() && staticEval + config.getFpMargin()[depth] < alpha && !isInCheck && isQuiet) {
                board.unmakeMove();
                continue;
            }

            // In certain interesting cases (e.g. promotions, or checks that do not immediately lose material), let's
            // extend the search depth by one ply.
            // See https://www.chessprogramming.org/Extensions
            int extension = 0;
            if (isPromotion || (isCheck && see.evaluateAfterMove(board, move) >= 0)) {
                extension = 1;
            }

            int eval;
            if (isDraw()) {
                eval = Score.DRAW_SCORE;
            }
            else if (pvNode && i == 0) {
                // The first move must be searched with the full alpha-beta window. If our move ordering is any good
                // then we expect this to be the best move, and so we need to retrieve the exact score.
                // See https://www.chessprogramming.org/Principal_Variation_Search
                eval = -search(depth - 1 + extension, ply + 1, -beta, -alpha, true);
            }
            else {
                // If the move is ordered very late in the list, and isn't a 'noisy' move like a check, capture or
                // promotion, let's assume it's less likely to be good, and fully skip searching that move.
                // See https://www.chessprogramming.org/Futility_Pruning#Move_Count_Based_Pruning
                if (!pvNode
                    && !isInCheck
                    && isQuiet
                    && depth <= config.getLmpDepth()
                    && i >= lmpMoveCount) {
                    board.unmakeMove();
                    continue;
                }
                // If the move is ordered late in the list, and isn't a 'noisy' move like a check, capture or promotion,
                // let's save time by assuming it's less likely to be good, and reduce the search depth.
                // See https://www.chessprogramming.org/Late_Move_Reductions
                int reduction = 0;
                if (depth >= config.getLmrDepth()
                    && i >= (pvNode ? config.getLmrMinSearchedMoves() : config.getLmrMinSearchedMoves() - 1)
                    && isQuiet) {
                    reduction = config.getLmrReductions()[depth][i];
                    if (pvNode || isInCheck) {
                        reduction--;
                    }
                    reduction = Math.max(0, reduction);
                }

                // For all other moveEntities apart from the principal variation, search with a null window (-alpha - 1, -alpha),
                // to try and prove the move will fail low while saving the time spent on a full search.
                eval = -search(depth - 1 + extension - reduction, ply + 1, -alpha - 1, -alpha, true);

                if (eval > alpha && (eval < beta || reduction > 0)) {
                    // If we reduced the depth and/or used a null window, and the score beat alpha, we need to do a
                    // re-search with the full window and depth. This is costly, but hopefully doesn't happen too often.
                    eval = -search(depth - 1 + extension, ply + 1, -beta, -alpha, true);
                }
            }

            board.unmakeMove();

            if (isCancelled()) {
                return alpha;
            }

            if (eval >= beta) {
                // This is a beta cut-off - the opponent won't let us get here as they already have better alternatives
                transpositionTable.put(getKey(), HashFlag.LOWER, depth, ply, move, beta);
                if (!isCapture) {
                    // Non-captures which cause a beta cut-off are stored as 'killer' and 'history' moveEntities for future move ordering
                    moveOrderer.addKillerMove(ply, move);
                    moveOrderer.addHistoryMove(depth, move, board.isWhiteToMove());
                }
                return beta;
            }

            if (eval > alpha) {
                // We have found a new best move
                bestMove = move;
                alpha = eval;
                flag = HashFlag.EXACT;
                if (ply == 0) {
                    bestMoveCurrentDepth = move;
                    bestEvalCurrentDepth = eval;
                }
            }
        }
        transpositionTable.put(getKey(), flag, depth, ply, bestMove, alpha);
        return alpha;

    }

    /**
     * Extend the search by searching captures until a 'quiet' position is reached, where there are no further captures
     * and therefore limited potential for winning tactics that drastically alter the evaluation. Used to mitigate the
     * worst of the 'horizon effect'.
     *
     * @see <a href="https://www.chessprogramming.org/Quiescence_Search">Chess Programming Wiki</a>
     */
    int quiescenceSearch(int alpha, int beta, int depth, int ply) {
        if (isCancelled()) {
            return alpha;
        }
        // Exit the quiescence search early if we already have an accurate score stored in the hash table.
        HashEntry transposition = transpositionTable.get(getKey(), ply);
        if (isUsefulTransposition(transposition, 1, alpha, beta)) {
            return transposition.getScore();
        }
        Move previousBestMove = transposition != null ? transposition.getMove() : null;

        boolean isInCheck = moveGenerator.isCheck(board, board.isWhiteToMove());

        // When in check there is no option to 'stand pat', so the static evaluation is not needed.
        int eval = isInCheck ? 0 : evaluate();
        int standPat = eval;

        List<Move> moves;
        if (isInCheck) {
            // If we are in check, we need to generate 'all' legal moveEntities that evade check, not just captures. Otherwise,
            // we risk missing simple mate threats.
            moves = moveGenerator.generateMoves(board, MoveFilter.ALL);
            if (moves.isEmpty()) {
                return -Score.MATE_SCORE + ply;
            }
        } else {
            // If we are not in check, then we have the option to 'stand pat', i.e. decline to continue the capture chain,
            // if the static evaluation of the position is good enough.
            if (eval >= beta) {
                return beta;
            }
            if (eval > alpha) {
                alpha = eval;
            }
            MoveFilter filter = depth == 1 ? MoveFilter.CAPTURES_AND_CHECKS : MoveFilter.CAPTURES_ONLY;
            moves = moveGenerator.generateMoves(board, filter);
        }

        int[] scores = new int[moves.size()];
        for (int i = 0; i < moves.size(); i++) {
            scores[i] = moveOrderer.mvvLva(board, moves.get(i), previousBestMove);
        }

        for (int i = 0; i < moves.size(); i++) {
            Move move = selectNextMove(moves, scores, i);
            if (!isInCheck) {
                // If the captured piece + a margin still has no potential of raising alpha, let's assume this position
                // is bad for us no matter what we do, and not bother searching any further
                // See https://www.chessprogramming.org/Delta_Pruning
                Piece capturedPiece = move.isEnPassant() ? Piece.PAWN : board.pieceAt(move.getEndSquare());
                boolean isFutile = capturedPiece != null && (standPat + capturedPiece.getValue() + config.getDpMargin() < alpha) && !move.isPromotion();
                if (isFutile) {
                    continue;
                }
                // Evaluate the possible captures + recaptures on the target square, in order to ilter out losing capture
                // chains, such as capturing with the queen a pawn defended by another pawn.
                // See https://www.chessprogramming.org/Static_Exchange_Evaluation
                int seeScore = see.evaluate(board, move);
                boolean isBadCapture = (depth <= 3 && seeScore < 0) || (depth > 3 && seeScore <= 0);
                if (isBadCapture) {
                    continue;
                }
            }

            board.makeMove(move);
            nodes++;
            eval = -quiescenceSearch(-beta, -alpha, depth + 1, ply + 1);
            board.unmakeMove();

            if (eval >= beta) {
                return beta;
            }
            if (eval > alpha) {
                alpha = eval;
            }
        }

        return alpha;

    }

    @Override
    public void setPosition(Board board) {
        this.board = board;
        this.evaluator.evaluate(board);
    }

    @Override
    public void setHashSize(int hashSizeMb) {
        this.transpositionTable = new TranspositionTable(hashSizeMb);
    }

    @Override
    public void setThreadCount(int threadCount) {
        // do nothing as this implementation is single-threaded
    }

    public void setStopSignal(AtomicBoolean stopSignal) {
        this.stopSignal = stopSignal;
    }

    public void setThreadIndex(int threadIndex) {
        this.threadIndex = threadIndex;
    }

    /**
     * Check if the hit from the transposition table is 'useful' in the current search. A TT-hit is useful either if it
     * 1) contains an exact evaluation, so we don't need to search any further, 2) contains a fail-high greater than our
     * current beta value, or 3) contains a fail-low lesser than our current alpha value.
     */
    private boolean isUsefulTransposition(HashEntry entry, int depth, int alpha, int beta) {
        return entry != null &&
                entry.getDepth() >= depth &&
                ((entry.getFlag().equals(HashFlag.EXACT)) ||
                 (entry.getFlag().equals(HashFlag.UPPER) && entry.getScore() <= alpha) ||
                 (entry.getFlag().equals(HashFlag.LOWER) && entry.getScore() >= beta));
    }

    /**
     * Find the pieces of the side to move which are the only blocker between one of their own sliders and the opponent
     * king, and so would give a discovered check by moving.
     */
    private long calculateDiscoveredCheckers(Board board, boolean isWhite, int opponentKing, long occupied) {
        long diagonalSliders = board.getBishops(isWhite) | board.getQueens(isWhite);
        long orthogonalSliders = board.getRooks(isWhite) | board.getQueens(isWhite);
        long snipers = (MagicBitboards.bishopAttacks(opponentKing, 0L) & diagonalSliders)
                | (MagicBitboards.rookAttacks(opponentKing, 0L) & orthogonalSliders);
        long friendlies = board.getPieces(isWhite);
        long discoveredCheckers = 0L;
        while (snipers != 0) {
            int sniper = BitOperations.getNextBit(snipers);
            long blockers = rayCalculator.rayBetween(opponentKing, sniper) & occupied;
            if (BitOperations.countBits(blockers) == 1 && (blockers & friendlies) != 0) {
                discoveredCheckers |= blockers;
            }
            snipers = BitOperations.popBit(snipers);
        }
        return discoveredCheckers;
    }

    /**
     * Select the highest-scoring move from index {@code from} onwards and swap it into position {@code from}.
     */
    private Move selectNextMove(List<Move> moves, int[] scores, int from) {
        int bestIndex = from;
        int bestScore = scores[from];
        for (int j = from + 1; j < scores.length; j++) {
            if (scores[j] > bestScore) {
                bestScore = scores[j];
                bestIndex = j;
            }
        }
        Move move = moves.get(bestIndex);
        if (bestIndex != from) {
            scores[bestIndex] = scores[from];
            scores[from] = bestScore;
            moves.set(bestIndex, moves.get(from));
            moves.set(from, move);
        }
        return move;
    }

    private boolean isCheckmateFoundAtCurrentDepth(int currentDepth) {
        return Math.abs(bestEval) >= Score.MATE_SCORE - currentDepth;
    }

    private SearchResult buildResult() {
        long millis = (System.nanoTime() - startNanos) / 1_000_000;
        long nps = nodes > 0 && millis > 0 ? ((nodes / millis) * 1000) : 0;
        return new SearchResult(bestEval, bestMove, currentDepth, millis, nodes, nps);
    }

    private boolean isCancelled() {
        // Exit if local search is cancelled
        if (cancelled) return true;
        // Exit if global search is cancelled, or the main Lazy SMP thread has finished
        if (config.isSearchCancelled() || (stopSignal != null && stopSignal.get())) {
            cancelled = true;
            return true;
        }
        // Only consult the clock periodically, as reading it is expensive relative to a single node
        if (++timeCheckCounter < TIME_CHECK_INTERVAL) return false;
        timeCheckCounter = 0;
        if (!config.isPondering() && System.nanoTime() - deadlineNanos >= 0) {
            cancelled = true;
        }
        return cancelled;
    }

    /**
     * Statically evaluate the current position, using the evaluation cache when possible.
     */
    private int evaluate() {
        long key = getKey();
        int index = (int) (key & (EVAL_CACHE_SIZE - 1));
        long entry = evalCache[index];
        if (entry != 0 && ((entry ^ key) & EVAL_CACHE_KEY_MASK) == 0) {
            // Sign-extend the 24-bit evaluation
            return ((int) ((entry & EVAL_CACHE_SCORE_MASK) << 8)) >> 8;
        }
        int eval = evaluator.evaluate(board);
        evalCache[index] = (key & EVAL_CACHE_KEY_MASK) | (eval & EVAL_CACHE_SCORE_MASK);
        return eval;
    }

    private boolean isDraw() {
        return Result.isEffectiveDraw(board);
    }

    private long getKey() {
        return board.getGameState().getZobristKey();
    }


    @Override
    public TranspositionTable getTranspositionTable() {
        return transpositionTable;
    }

    @Override
    public void clearHistory() {
        transpositionTable.clear();
    }

    @Override
    public void logStatistics() {
        //log.info(statistics.generateReport());
    }
}
