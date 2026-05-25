package com.fiap.vinshare.infra.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * Verifica HMAC-SHA256 do corpo da requisição em endpoints sensíveis.
 * Atende Cyber Frente 3 (assinatura/verificação de integridade de payloads, 5 pts).
 *
 * Endpoints protegidos são configurados via
 * {@code security.payload-signature.protected-paths}, no formato
 * {@code METHOD /padrao,METHOD /padrao}, ex.: {@code PATCH /appointments/* /complete}.
 */
@Slf4j
@Component
public class PayloadIntegrityFilter extends OncePerRequestFilter {

    @Value("${security.payload-signature.key:}")
    private String signingKey;

    @Value("${security.payload-signature.header:X-Request-Signature}")
    private String headerName;

    @Value("${security.payload-signature.protected-paths:}")
    private String protectedPathsRaw;

    private final AntPathMatcher matcher = new AntPathMatcher();
    private List<Rule> rules = List.of();

    @PostConstruct
    void init() {
        if (protectedPathsRaw == null || protectedPathsRaw.isBlank()) {
            log.info("PayloadIntegrityFilter desabilitado (nenhum protected-path configurado).");
            return;
        }
        rules = Arrays.stream(protectedPathsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(spec -> {
                    String[] parts = spec.split("\\s+", 2);
                    if (parts.length != 2) {
                        throw new IllegalStateException("Formato inválido em protected-paths: " + spec);
                    }
                    return new Rule(parts[0].toUpperCase(), parts[1]);
                })
                .toList();
        log.info("PayloadIntegrityFilter ativo para: {}", rules);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!matches(request) || signingKey == null || signingKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
        // Garante leitura do body antes da verificação
        wrapped.getInputStream().readAllBytes();
        byte[] body = wrapped.getContentAsByteArray();

        String provided = request.getHeader(headerName);
        if (!StringUtils.hasText(provided)) {
            reject(response, "Cabeçalho de assinatura ausente");
            return;
        }
        String expected = "hmac-sha256=" + computeHmac(body);
        if (!constantTimeEquals(expected, provided)) {
            log.warn("Assinatura inválida em {} {}", request.getMethod(), request.getRequestURI());
            reject(response, "Assinatura inválida");
            return;
        }
        chain.doFilter(wrapped, response);
    }

    private boolean matches(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase();
        String path = request.getRequestURI();
        return rules.stream().anyMatch(r -> r.method.equals(method) && matcher.match(r.pattern, path));
    }

    private String computeHmac(byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular HMAC", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }

    private void reject(HttpServletResponse response, String detail) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"https://api.fordvinshare.fiap/errors/401",
                 "title":"Assinatura inválida",
                 "status":401,
                 "detail":"%s"}
                """.formatted(detail));
    }

    private record Rule(String method, String pattern) {}
}
