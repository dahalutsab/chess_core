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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Implementation of {@link Search} using Lazy SMP (Symmetric Multi-Processing) algorithm.
 * Employs multiple threads to search the same position concurrently while sharing a single
 * {@link TranspositionTable}. Each thread runs an independent iterative deepening search
 * while benefiting from entries stored in the shared table by other threads.
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

    private List<Searcher> searchers;

    public LazySMPSearcher(EngineConfig config,
                           Supplier<MoveGeneration> moveGeneratorSupplier,
                           Supplier<MoveOrdering> moveOrdererSupplier,
                           Supplier<Evaluation> evaluatorSupplier,
                           TranspositionTable transpositionTable) {
        this.config = config;
        this.hashSize = config.getDefaultHashSizeMb();
        this.threadCount = config.getDefaultThreadCount();
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
            List<CompletableFuture<SearchResult>> threads = searchers.stream()
                    .map(searcher -> CompletableFuture.supplyAsync(() -> searcher.negamax(duration)))
                    .toList();
            SearchResult result = selectResult(threads).get();
            threads.forEach(thread -> thread.cancel(true));
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
        this.threadCount = threadCount;
        this.searchers = initSearchers();
    }

    /**
     * Combines the {@link SearchResult} results of the different threads and selects a final result to use.
     * Simply selects the result from the thread which searched to the greatest depth.
     */
    private CompletableFuture<SearchResult> selectResult(List<CompletableFuture<SearchResult>> threads) {
        CompletableFuture<SearchResult> collector = CompletableFuture.completedFuture(new SearchResult(0, null, 0, 0, 0, 0));
        for (CompletableFuture<SearchResult> thread : threads) {
            collector = collector.thenCombine(thread, (thread1, thread2) -> thread1.depth() > thread2.depth() ? thread1 : thread2);
        }
        return collector;
    }

    private List<Searcher> initSearchers() {
        return IntStream.range(0, threadCount).mapToObj(i -> initSearcher()).toList();
    }

    private Searcher initSearcher() {
        MoveGeneration moveGenerator = this.moveGeneratorSupplier.get();
        MoveOrdering moveOrderer = this.moveOrdererSupplier.get();
        Evaluation evaluator = this.evaluatorSupplier.get();
        return new Searcher(config, threadManager, moveGenerator, moveOrderer, evaluator, transpositionTable);
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
