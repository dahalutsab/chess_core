package com.utsab.chess_engine.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Remembers when the API last served a real request. Background work that touches the database (the abandoned-game
 * sweep) pauses while nobody is using the app, so a database that sleeps when idle (e.g. Neon's free plan) can.
 * Health checks don't count as use.
 *
 * @author Utsab Dahal
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ActivityTracker extends OncePerRequestFilter {

    public static final String HEALTH_PATH = "/api/v1/health";
    /** Request attribute holding when the request reached the server, in epoch milliseconds */
    public static final String RECEIVED_AT = "uchess.receivedAt";

    private static volatile long lastActivity = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        request.setAttribute(RECEIVED_AT, System.currentTimeMillis());
        if (!HEALTH_PATH.equals(request.getRequestURI())) {
            lastActivity = System.currentTimeMillis();
        }
        chain.doFilter(request, response);
    }

    /** How long since the last request that wasn't a health check */
    public static Duration idleFor() {
        return Duration.ofMillis(System.currentTimeMillis() - lastActivity);
    }
}
