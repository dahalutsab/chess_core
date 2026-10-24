package com.utsab.chess_engine.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pushes events to browsers over Server-Sent Events, so clients learn about moves, challenges and messages as they
 * happen instead of polling for them.
 * <p>
 * Each stream subscribes to one topic: a game ({@link #gameTopic}) or a user ({@link #userTopic}). Events are small
 * notifications; clients fetch the current state through the normal API when one arrives.
 *
 * @author Utsab Dahal
 */
@Slf4j
@Component
public class EventHub {

    /** Streams are closed after this long, and clients reconnect, so dead connections don't linger */
    private static final long STREAM_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<String, List<SseEmitter>> topics = new ConcurrentHashMap<>();

    public static String gameTopic(UUID gameId) {
        return "game:" + gameId;
    }

    public static String userTopic(Long userId) {
        return "user:" + userId;
    }

    public SseEmitter subscribe(String topic) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        List<SseEmitter> emitters = topics.computeIfAbsent(topic, t -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        Runnable remove = () -> remove(topic, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        try {
            // Tells the client the stream is open, so it can catch up on anything it missed while connecting
            emitter.send(SseEmitter.event().name("ready").data("{}", MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            remove.run();
        }
        return emitter;
    }

    /**
     * Send an event to everyone subscribed to the topic. Inside a transaction, the event goes out once it commits,
     * so a client reacting to it reads the new state.
     */
    public void publish(String topic, String name, Object data) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(topic, name, data);
                }
            });
        } else {
            send(topic, name, data);
        }
    }

    private void send(String topic, String name, Object data) {
        List<SseEmitter> emitters = topics.get(topic);
        if (emitters == null) return;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                remove(topic, emitter);
            }
        }
    }

    /** Keeps idle connections open through proxies, and finds streams whose client has gone */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        topics.forEach((topic, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    remove(topic, emitter);
                }
            }
        });
    }

    private void remove(String topic, SseEmitter emitter) {
        topics.computeIfPresent(topic, (t, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
