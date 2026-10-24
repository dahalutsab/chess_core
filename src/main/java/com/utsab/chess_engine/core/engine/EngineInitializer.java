package com.utsab.chess_engine.core.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utsab.chess_engine.core.evaluation.Evaluation;
import com.utsab.chess_engine.core.evaluation.Evaluator;
import com.utsab.chess_engine.core.generation.MoveGeneration;
import com.utsab.chess_engine.core.generation.MoveGenerator;
import com.utsab.chess_engine.core.opening.OpeningBook;
import com.utsab.chess_engine.core.search.LazySMPSearcher;
import com.utsab.chess_engine.core.search.Search;
import com.utsab.chess_engine.core.search.moveordering.MoveOrderer;
import com.utsab.chess_engine.core.search.moveordering.MoveOrdering;
import com.utsab.chess_engine.core.transposition.TranspositionTable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Initializes the chess engine by loading default or custom configurations and opening books, and assembling the engine
 * components. Provides factory methods for creating engine instances and their dependencies.
 *
 * @author Utsab Dahal
 * @version 1.0
 * @since 2025
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EngineInitializer {

    static final String DEFAULT_CONFIG_LOCATION = "/uchess_config.json";
    static final String DEFAULT_BOOK_LOCATION = "/opening_book.txt";
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Creates and configures a new chess engine instance with default settings and components.
     *
     * @return a fully initialized chess engine
     * @throws RuntimeException if configuration or opening book loading fails
     */
    public static Engine loadEngine() {
        EngineConfig config = loadDefaultConfig();
        config.postInitialise();
        OpeningBook book = loadDefaultOpeningBook();
        Supplier<MoveGeneration> moveGenerator = MoveGenerator::new;
        Supplier<MoveOrdering> moveOrderer = MoveOrderer::new;
        Supplier<Evaluation> evaluator = () -> new Evaluator(config);
        TranspositionTable transpositionTable = new TranspositionTable(config.getDefaultHashSizeMb());
        Search searcher = new LazySMPSearcher(config, moveGenerator, moveOrderer, evaluator, transpositionTable);
        Engine engine = new Engine(config, book, moveGenerator.get(), searcher);

        // The engine is created before Spring starts, so it reads its deployment settings from the environment.
        // On a small host one search thread does as well as several, and uses less memory.
        Integer threads = intFromEnvironment("UCHESS_ENGINE_THREADS");
        if (threads != null) engine.setThreadCount(threads);
        Integer hashMb = intFromEnvironment("UCHESS_ENGINE_HASH_MB");
        if (hashMb != null) engine.setHashSize(hashMb);
        return engine;
    }

    private static Integer intFromEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Loads the default engine configuration from the default JSON resource.
     *
     * @return the loaded engine configuration
     * @throws RuntimeException if the configuration file cannot be read
     */
    public static EngineConfig loadDefaultConfig() {
        return loadConfig(DEFAULT_CONFIG_LOCATION);
    }

    /**
     * Loads an engine configuration from the specified JSON resource.
     *
     * @param configLocation the path to the JSON configuration file
     * @return the loaded engine configuration
     * @throws RuntimeException if the configuration file cannot be read
     */
    public static EngineConfig loadConfig(String configLocation) {
        try (InputStream inputStream = EngineInitializer.class.getResourceAsStream(configLocation)) {
            EngineConfig config = OBJECT_MAPPER.readValue(inputStream, EngineConfig.class);
            config.postInitialise();
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads the default opening book from the default resource.
     *
     * @return the loaded opening book, or null if the resource is not found
     * @throws RuntimeException if the opening book file cannot be read
     */
    public static OpeningBook loadDefaultOpeningBook() {
        return loadOpeningBook(DEFAULT_BOOK_LOCATION);
    }

    /**
     * Loads an opening book from the specified resource.
     *
     * @param bookLocation the path to the opening book file
     * @return the loaded opening book, or null if the resource is not found
     * @throws RuntimeException if the opening book file cannot be read
     */
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