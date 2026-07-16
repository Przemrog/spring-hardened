package com.example.notes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [HARDENING A06/A07] prosty, wbudowany w aplikacje limiter prob logowania.
// UWAGA metodologiczna: Spring nie ma odpowiednika .NET AddRateLimiter w rdzeniu frameworka -
// wymaga wlasnego kodu lub zewnetrznej biblioteki (np. Bucket4j). Ta klasa to koszt utwardzenia
// widoczny w PB3/H2 (kilkadziesiat linii wlasnego kodu vs. konfiguracja w .NET).
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int LIMIT = 5;
    private static final long WINDOW_MS = 30_000;
    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(
            LoginRateLimitFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        boolean isLogin = "POST".equalsIgnoreCase(req.getMethod())
                && (req.getRequestURI().equals("/login") || req.getRequestURI().equals("/api/login"));
        if (isLogin) {
            String key = req.getRemoteAddr();
            long now = System.currentTimeMillis();
            Deque<Long> q = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
            synchronized (q) {
                while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS)
                    q.pollFirst();
                if (q.size() >= LIMIT) {
                    log.warn(
                            "SECURITY event=rate_limit_exceeded "
                                    + "ip={} method={} path={} "
                                    + "attempts={} window_ms={}",
                            key,
                            req.getMethod(),
                            req.getRequestURI(),
                            q.size(),
                            WINDOW_MS);

                    res.setStatus(
                            HttpStatus.TOO_MANY_REQUESTS.value());

                    res.getWriter().write(
                            "Zbyt wiele prób logowania. Spróbuj później.");

                    return;
                }
                q.addLast(now);
            }
        }
        chain.doFilter(req, res);
    }
}
