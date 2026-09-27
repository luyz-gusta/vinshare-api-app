package com.fiap.vinshare.infra.security;

import com.fiap.vinshare.infra.errors.exceptions.TooManyRequestsException;
import com.fiap.vinshare.service.AuditService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bloqueio de força bruta por e-mail (complementa o rate limit por IP, que
 * não pega ataques distribuídos). Conta falhas numa janela fixa; ao atingir o
 * limite, audita SUSPICIOUS_LOGIN e responde 429 até a janela expirar.
 * Conta também e-mails inexistentes, para não revelar quais estão cadastrados.
 */
@Service
public class LoginAttemptService {

    private final int threshold;
    private final Duration window;
    private final AuditService auditService;
    private final SecurityEvents securityEvents;
    private final Cache<String, AtomicInteger> failures;

    public LoginAttemptService(@Value("${security.audit.login-failure-threshold:5}") int threshold,
                               @Value("${security.audit.login-failure-window-minutes:10}") int windowMinutes,
                               AuditService auditService,
                               SecurityEvents securityEvents) {
        this.threshold = threshold;
        this.window = Duration.ofMinutes(windowMinutes);
        this.auditService = auditService;
        this.securityEvents = securityEvents;
        this.failures = Caffeine.newBuilder()
                .expireAfterWrite(window)
                .maximumSize(100_000)
                .build();
    }

    public void checkAllowed(String email) {
        AtomicInteger count = failures.getIfPresent(email);
        if (count != null && count.get() >= threshold) {
            securityEvents.loginBlocked();
            throw new TooManyRequestsException(
                    "Muitas tentativas de login para esta conta. Tente novamente mais tarde.", window.toSeconds());
        }
    }

    public void onFailure(String email) {
        int attempts = failures.get(email, k -> new AtomicInteger()).incrementAndGet();
        if (attempts == threshold) {
            auditService.suspiciousLogin(email, attempts);
        }
    }

    public void onSuccess(String email) {
        failures.invalidate(email);
    }
}
