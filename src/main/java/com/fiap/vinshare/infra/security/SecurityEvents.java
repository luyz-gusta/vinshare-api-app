package com.fiap.vinshare.infra.security;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Ponto único para eventos de segurança: incrementa métricas Micrometer
 * (exportadas ao Application Insights) e grava log WARN estruturado com o
 * campo "event", consultável no Log Analytics e usado nos alertas.
 * Nunca registra e-mail, senha ou token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityEvents {

    private final MeterRegistry registry;

    public void loginSucceeded() {
        registry.counter("vinshare.security.login", "outcome", "success").increment();
    }

    public void loginFailed() {
        registry.counter("vinshare.security.login", "outcome", "failure").increment();
        log.warn("Falha de login {}", kv("event", "LOGIN_FAILURE"));
    }

    public void loginBlocked() {
        registry.counter("vinshare.security.login", "outcome", "blocked").increment();
        log.warn("Login bloqueado por excesso de tentativas {}", kv("event", "SUSPICIOUS_LOGIN"));
    }

    public void rejected(HttpServletRequest request, int status, String reason) {
        registry.counter("vinshare.security.rejected", "status", String.valueOf(status)).increment();
        log.warn("Requisição rejeitada {} {} {} {} {}",
                kv("event", status == 401 ? "AUTH_REJECTED" : "ACCESS_DENIED"),
                kv("status", status), kv("method", request.getMethod()),
                kv("path", request.getRequestURI()), kv("reason", reason));
    }

    public void rateLimited(HttpServletRequest request, String scope, String ip) {
        registry.counter("vinshare.security.rate_limited", "scope", scope).increment();
        log.warn("Rate limit excedido {} {} {} {}",
                kv("event", "RATE_LIMITED"), kv("scope", scope), kv("ip", ip), kv("path", request.getRequestURI()));
    }

    public void refreshTokenReuse() {
        registry.counter("vinshare.security.refresh_reuse").increment();
        log.warn("Reuso de refresh token detectado {}", kv("event", "REFRESH_TOKEN_REUSE"));
    }
}
