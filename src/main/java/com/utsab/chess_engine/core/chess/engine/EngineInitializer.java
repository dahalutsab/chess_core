package com.utsab.chess_engine.core.chess.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utsab.chess_engine.core.chess.evaluation.Evaluation;
import com.utsab.chess_engine.core.chess.evaluation.Evaluator;
import com.utsab.chess_engine.core.chess.generation.MoveGeneration;
import com.utsab.chess_engine.core.chess.generation.MoveGenerator;
import com.utsab.chess_engine.core.chess.opening.OpeningBook;
import com.utsab.chess_engine.core.chess.search.ParallelSearcher;
import com.utsab.chess_engine.core.chess.search.Search;
import com.utsab.chess_engine.core.chess.search.moveordering.MoveOrderer;
import com.utsab.chess_engine.core.chess.search.moveordering.MoveOrdering;
import com.utsab.chess_engine.core.chess.transposition.TranspositionTable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EngineInitializer {

    static final String DEFAULT_CONFIG_LOCATION = "/engine_config.json";
    static final String DEFAULT_BOOK_LOCATION = "/opening_book.txt";
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Engine loadEngine() {
        EngineConfig config = loadDefaultConfig();
        config.postInitialise();
        OpeningBook book = loadDefaultOpeningBook();
        Supplier<MoveGeneration> moveGenerator = MoveGenerator::new;
        Supplier<MoveOrdering> moveOrderer = MoveOrderer::new;
        Supplier<Evaluation> evaluator = () -> new Evaluator(config);
        TranspositionTable transpositionTable = new TranspositionTable(config.getDefaultHashSizeMb());
        Search searcher = new ParallelSearcher(config, moveGenerator, moveOrderer, evaluator, transpositionTable);
        return new Engine(config, book, moveGenerator.get(), searcher);
    }

    public static EngineConfig loadDefaultConfig() {
        return loadConfig(DEFAULT_CONFIG_LOCATION);
    }

    public static EngineConfig loadConfig(String configLocation) {
        try (InputStream inputStream = EngineInitializer.class.getResourceAsStream(configLocation)) {
            EngineConfig config = OBJECT_MAPPER.readValue(inputStream, EngineConfig.class);
            config.postInitialise();
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static OpeningBook loadDefaultOpeningBook() {
        return loadOpeningBook(DEFAULT_BOOK_LOCATION);
    }

    public static OpeningBook loadOpeningBook(String bookLocation) {
        try (InputStream inputStream = EngineInitializer.class.getResourceAsStream(bookLocation)) {
            if (inputStream == null) {
                return null;
            }
            String file = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            return new OpeningBook(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
