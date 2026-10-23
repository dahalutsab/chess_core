package com.utsab.chess_engine.core.search;

import com.utsab.chess_engine.core.board.Board;
import com.utsab.chess_engine.core.engine.EngineConfig;
import com.utsab.chess_engine.core.evaluation.Evaluation;
import com.utsab.chess_engine.core.generation.MoveGeneration;
import com.utsab.chess_engine.core.search.moveordering.MoveOrdering;
import com.utsab.chess_engine.core.transposition.TranspositionTable;
import com.utsab.chess_engine.core.utils.BoardUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Implementation of {@link Search} using Lazy SMP (Symmetric Multi-Processing) algorithm.
 * Employs multiple threads to search the same position concurrently while sharing a single
 * {@link TranspositionTable}. Each thread runs an independent iterative deepening search
 * while benefiting from entries stored in the shared table by other threads.
 * <p>
 * The main search runs on the calling thread, and helpers run on a dedicated thread pool. Helpers previously ran on
 * the shared {@code ForkJoinPool.commonPool()}, where they could be queued behind other work and start late, which made
 * a 1-second search take several seconds. When the main thread finishes, a shared stop signal ends all helpers.
 *
 * @see <a href="https://www.chessprogramming.org/Lazy_SMP">Chess Programming Wiki - Lazy SMP</a>
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LazySMPSearcher implements Search {

    final EngineConfig config;
    final Supplier<MoveGeneration> moveGeneratorSupplier;
    final Supplier<MoveOrdering> moveOrdererSupplier;
    final Supplier<Evaluation> evaluatorSupplier;
    ThreadManager threadManager;
    TranspositionTable transpositionTable;
    int threadCount;
    int hashSize;
    Board board;

    final AtomicBoolean stopSignal = new AtomicBoolean();
    final ExecutorService helperPool = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "uchess-search-helper");
        thread.setDaemon(true);
        return thread;
    });

    private List<Searcher> searchers;

    public LazySMPSearcher(EngineConfig config,
                           Supplier<MoveGeneration> moveGeneratorSupplier,
                           Supplier<MoveOrdering> moveOrdererSupplier,
                           Supplier<Evaluation> evaluatorSupplier,
                           TranspositionTable transpositionTable) {
        this.config = config;
        this.hashSize = config.getDefaultHashSizeMb();
        this.threadCount = clampThreadCount(config.getDefaultThreadCount());
        this.threadManager = new ThreadManager();
        this.transpositionTable = transpositionTable;
        this.moveGeneratorSupplier = moveGeneratorSupplier;
        this.moveOrdererSupplier = moveOrdererSupplier;
        this.evaluatorSupplier = evaluatorSupplier;
        this.searchers = initSearchers();
    }

    @Override
    public SearchResult negamax(Duration duration) {
        try {
            setPosition(board);
            threadManager.reset();
            stopSignal.set(false);
            transpositionTable.incrementAge();

            List<Future<SearchResult>> helpers = new ArrayList<>();
            for (int i = 1; i < searchers.size(); i++) {
                Searcher helper = searchers.get(i);
                helpers.add(helperPool.submit(() -> helper.negamax(duration)));
            }
            SearchResult result = searchers.getFirst().negamax(duration);
            stopSignal.set(true);

            for (Future<SearchResult> helper : helpers) {
                result = selectResult(result, helper.get());
            }
            return result;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPosition(Board board) {
        this.board = board;
        searchers.forEach(searcher -> searcher.setPosition(BoardUtils.copy(board)));
    }

    @Override
    public void setHashSize(int hashSizeMb) {
        this.hashSize = hashSizeMb;
        this.transpositionTable = new TranspositionTable(this.hashSize);
        this.searchers = initSearchers();
    }

    @Override
    public void setThreadCount(int threadCount) {
        this.threadCount = clampThreadCount(threadCount);
        this.searchers = initSearchers();
    }

    /**
     * Combines the {@link SearchResult} results of two threads and selects a final result to use. Prefers the main
     * thread's result, unless a helper thread searched to a greater depth.
     */
    private SearchResult selectResult(SearchResult main, SearchResult helper) {
        if (helper == null || helper.move() == null) return main;
        if (main == null || main.move() == null) return helper;
        return helper.depth() > main.depth() ? helper : main;
    }

    /**
     * Never run more search threads than there are processors, otherwise threads compete for CPU time and every
     * thread searches more slowly.
     */
    private int clampThreadCount(int requestedThreadCount) {
        int processors = Runtime.getRuntime().availableProcessors();
        return Math.max(1, Math.min(requestedThreadCount, processors));
    }

    private List<Searcher> initSearchers() {
        return IntStream.range(0, threadCount).mapToObj(this::initSearcher).toList();
    }

    private Searcher initSearcher(int threadIndex) {
        MoveGeneration moveGenerator = this.moveGeneratorSupplier.get();
        MoveOrdering moveOrderer = this.moveOrdererSupplier.get();
        Evaluation evaluator = this.evaluatorSupplier.get();
        Searcher searcher = new Searcher(config, threadManager, moveGenerator, moveOrderer, evaluator, transpositionTable);
        searcher.setStopSignal(stopSignal);
        searcher.setThreadIndex(threadIndex);
        return searcher;
    }

    @Override
    public TranspositionTable getTranspositionTable() {
        return transpositionTable;
    }

    @Override
    public void clearHistory() {
        transpositionTable.clear();
        searchers.forEach(Searcher::clearHistory);
    }

}
