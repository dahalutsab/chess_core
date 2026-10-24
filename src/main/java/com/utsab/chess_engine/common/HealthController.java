package com.utsab.chess_engine.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * For uptime monitors and the host's health check. It never touches the database, so frequent checks keep the server
 * awake without waking a database that sleeps when idle.
 */
@RestController
public class HealthController {

    @GetMapping(ActivityTracker.HEALTH_PATH)
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
