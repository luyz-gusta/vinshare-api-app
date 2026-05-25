package com.fiap.vinshare.infra.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit por IP em endpoints sensíveis usando Bucket4j em memória.
 * Atende Cyber Frente 3 (rate limiting/throttling).
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${security.rate-limit.default-capacity}")
    private int defaultCapacity;

    @Value("${security.rate-limit.default-refill-tokens}")
    private int defaultRefillTokens;

    @Value("${security.rate-limit.default-refill-minutes}")
    private int defaultRefillMinutes;

    @Value("${security.rate-limit.login-capacity}")
    private int loginCapacity;

    @Value("${security.rate-limit.login-refill-tokens}")
    private int loginRefillTokens;

    @Value("${security.rate-limit.login-refill-minutes}")
    private int loginRefillMinutes;

    @Value("${security.rate-limit.chat-capacity}")
    private int chatCapacity;

    @Value("${security.rate-limit.chat-refill-tokens}")
    private int chatRefillTokens;

    @Value("${security.rate-limit.chat-refill-minutes}")
    private int chatRefillMinutes;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = clientIp(request);
        String key = ip + "::" + bucketScope(path);

        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucketFor(path));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit excedido para {} em {}", ip, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("""
                    {"type":"https://api.fordvinshare.fiap/errors/429",
                     "title":"Muitas requisições",
                     "status":429,
                     "detail":"Aguarde antes de tentar novamente."}
                    """);
        }
    }

    private String bucketScope(String path) {
        if (path.endsWith("/auth/login")) return "login";
        if (path.contains("/chat/sessions/") && path.endsWith("/messages")) return "chat";
        return "default";
    }

    private Bucket newBucketFor(String path) {
        String scope = bucketScope(path);
        Bandwidth limit = switch (scope) {
            case "login" -> bandwidth(loginCapacity, loginRefillTokens, loginRefillMinutes);
            case "chat" -> bandwidth(chatCapacity, chatRefillTokens, chatRefillMinutes);
            default -> bandwidth(defaultCapacity, defaultRefillTokens, defaultRefillMinutes);
        };
        return Bucket.builder().addLimit(limit).build();
    }

    private Bandwidth bandwidth(int capacity, int refillTokens, int refillMinutes) {
        return Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(refillTokens, Duration.ofMinutes(refillMinutes))
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
