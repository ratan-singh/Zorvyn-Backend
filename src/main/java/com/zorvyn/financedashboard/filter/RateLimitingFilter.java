package com.zorvyn.financedashboard.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limiting filter for authentication endpoints using Bucket4j (in-memory).
 *
 * <p>Applies per-IP rate limits:
 * <ul>
 *   <li>POST /api/v1/auth/login    → 10 requests per minute</li>
 *   <li>POST /api/v1/auth/register → 5 requests per minute</li>
 * </ul>
 *
 * <p>Buckets are keyed by client IP (X-Forwarded-For → remoteAddr fallback)
 * and endpoint type (login vs register) to ensure independent limits.
 *
 * <p>This filter is completely self-contained with no Spring-managed bean
 * dependencies to avoid circular dependency issues.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String REGISTER_PATH = "/api/v1/auth/register";

    private static final int LOGIN_LIMIT_PER_MINUTE = 10;
    private static final int REGISTER_LIMIT_PER_MINUTE = 5;
    private static final int REFILL_DURATION_SECONDS = 60;
    private static final String RETRY_AFTER_SECONDS = "60";

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    /**
     * Per-IP bucket storage. Key format: "{ip}:{endpoint}" to ensure login and
     * register have independent rate limits per client.
     */
    private final ConcurrentHashMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate-limit auth endpoints
        if (!path.startsWith(AUTH_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = resolveBucket(clientIp, path);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, path);
            writeRateLimitResponse(response);
        }
    }

    /**
     * Resolves or creates a Bucket for the given IP + endpoint combination.
     * Login and register get independent buckets with different capacities.
     */
    private Bucket resolveBucket(String clientIp, String path) {
        String bucketKey = clientIp + ":" + normalizePath(path);
        return bucketCache.computeIfAbsent(bucketKey, key -> createBucket(path));
    }

    /**
     * Creates a new Bucket with the appropriate capacity based on the endpoint.
     * Uses greedy refill — tokens are added continuously, not in bursts.
     */
    private Bucket createBucket(String path) {
        int capacity = path.startsWith(REGISTER_PATH)
                ? REGISTER_LIMIT_PER_MINUTE
                : LOGIN_LIMIT_PER_MINUTE;

        Bandwidth bandwidth = Bandwidth.classic(
                capacity,
                Refill.greedy(capacity, Duration.ofSeconds(REFILL_DURATION_SECONDS)));

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * Normalizes the path to a bucket category to prevent path variations
     * from creating separate buckets (e.g., trailing slashes).
     */
    private String normalizePath(String path) {
        if (path.startsWith(REGISTER_PATH)) {
            return "register";
        }
        return "login";
    }

    /**
     * Resolves the client IP address, preferring X-Forwarded-For for
     * clients behind a reverse proxy (e.g., Nginx, AWS ALB).
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Writes a 429 Too Many Requests response directly to the servlet output stream.
     * Uses the existing ApiResponse envelope format with ISO-8601 timestamp string
     * to maintain response consistency across the entire API.
     */
    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HEADER_RETRY_AFTER, RETRY_AFTER_SECONDS);

        // Write JSON manually to avoid any dependency on Spring-managed ObjectMapper
        // and to guarantee the timestamp is an ISO-8601 string (not a Jackson array)
        String timestamp = LocalDateTime.now().toString();
        String jsonBody = String.format(
                "{\"success\":false,\"message\":\"Too many requests. Please slow down and try again later.\",\"timestamp\":\"%s\"}",
                timestamp);

        response.getWriter().write(jsonBody);
        response.getWriter().flush();
    }
}
