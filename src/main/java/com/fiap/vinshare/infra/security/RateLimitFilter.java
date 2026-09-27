package com.fiap.vinshare.infra.security;

import com.fiap.vinshare.infra.errors.ProblemDetailsWriter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate limit por IP real (ClientIpResolver) com Bucket4j em memória.
 * Buckets ficam num cache limitado com expiração, para que IPs forjados ou
 * muito variados não esgotem a memória.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ClientIpResolver ipResolver;
    private final ProblemDetailsWriter problemWriter;
    private final SecurityEvents securityEvents;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(15))
            .maximumSize(100_000)
            .build();

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
    // Refresh tem balde próprio: vários aparelhos atrás do mesmo IP (Wi-Fi, NAT) renovam a sessão
    // sem disputar o limite do login. O token de 384 bits não é adivinhável, então o limite só contém abuso.
    @Value("${security.rate-limit.refresh-capacity:60}")
    private int refreshCapacity;
    @Value("${security.rate-limit.refresh-refill-tokens:60}")
    private int refreshRefillTokens;
    @Value("${security.rate-limit.refresh-refill-minutes:1}")
    private int refreshRefillMinutes;
    @Value("${security.rate-limit.chat-capacity}")
    private int chatCapacity;
    @Value("${security.rate-limit.chat-refill-tokens}")
    private int chatRefillTokens;
    @Value("${security.rate-limit.chat-refill-minutes}")
    private int chatRefillMinutes;

    public RateLimitFilter(ClientIpResolver ipResolver, ProblemDetailsWriter problemWriter,
                           SecurityEvents securityEvents) {
        this.ipResolver = ipResolver;
        this.problemWriter = problemWriter;
        this.securityEvents = securityEvents;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String scope = bucketScope(request.getRequestURI());
        String ip = ipResolver.resolve(request);
        Bucket bucket = buckets.get(ip + "::" + scope, k -> newBucket(scope));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        securityEvents.rateLimited(request, scope, ip);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        problemWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "Muitas requisições",
                "Limite de requisições excedido. Aguarde %d segundos.".formatted(retryAfter));
    }

    private String bucketScope(String path) {
        if (path.endsWith("/auth/login")) return "login";
        if (path.endsWith("/auth/register")) return "register";
        if (path.endsWith("/auth/refresh")) return "refresh";
        if (path.contains("/chat/sessions/") && path.endsWith("/messages")) return "chat";
        return "default";
    }

    private Bucket newBucket(String scope) {
        Bandwidth limit = switch (scope) {
            case "login", "register" -> bandwidth(loginCapacity, loginRefillTokens, loginRefillMinutes);
            case "refresh" -> bandwidth(refreshCapacity, refreshRefillTokens, refreshRefillMinutes);
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
}
