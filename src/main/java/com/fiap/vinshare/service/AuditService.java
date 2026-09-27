package com.fiap.vinshare.service;

import com.fiap.vinshare.infra.security.ClientIpResolver;
import com.fiap.vinshare.domain.entities.AuditLog;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.infra.security.SecurityUtils;
import com.fiap.vinshare.repositories.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Registra eventos críticos na tabela audit_log (Cybersecurity, frente 5).
 *
 * Princípios:
 *  - Nunca lança exceção para o fluxo de negócio (auditoria não pode derrubar a requisição).
 *  - Grava em transação própria (REQUIRES_NEW) para sobreviver a rollback do chamador.
 *  - Nunca persiste dados pessoais sensíveis (CPF, senha, token) no payload.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String SUSPICIOUS_LOGIN = "SUSPICIOUS_LOGIN";
    public static final String LEAD_ACTION = "LEAD_ACTION";
    public static final String LEAD_CREATED = "LEAD_CREATED";
    public static final String BULK_QUERY = "BULK_QUERY";
    public static final String CONFIG_CHANGED = "CONFIG_CHANGED";
    public static final String NOTIFICATION_SENT = "NOTIFICATION_SENT";
    public static final String TOKEN_REUSE_DETECTED = "TOKEN_REUSE_DETECTED";

    private final AuditLogRepository auditLogRepository;
    private final ClientIpResolver clientIpResolver;

    @Value("${security.audit.bulk-query-threshold:100}")
    private int bulkQueryThreshold;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resourceType, UUID resourceId, Map<String, Object> payload) {
        try {
            HttpServletRequest request = currentRequest();
            AuditLog entry = AuditLog.builder()
                    .actorId(SecurityUtils.currentUser().map(User::getId).orElse(null))
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .payload(payload)
                    .ip(request != null ? clientIp(request) : null)
                    .userAgent(request != null ? truncate(request.getHeader("User-Agent"), 255) : null)
                    .correlationId(currentCorrelationId())
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Falha ao gravar audit_log para ação {}: {}", action, ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginSuccess(UUID userId, String email) {
        record(LOGIN_SUCCESS, "users", userId, Map.of("email", email));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginFailure(String email) {
        record(LOGIN_FAILURE, "users", null, Map.of("email", email == null ? "" : email));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void suspiciousLogin(String email, int attempts) {
        record(SUSPICIOUS_LOGIN, "users", null, Map.of("email", email == null ? "" : email, "attempts", attempts));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void leadAction(UUID customerId, String channel, String templateId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channel);
        if (templateId != null) payload.put("templateId", templateId);
        record(LEAD_ACTION, "customers", customerId, payload);
    }

    /**
     * Registra BULK_QUERY quando uma listagem retorna acima do threshold configurado.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkBulkQuery(String resourceType, long resultCount) {
        if (resultCount >= bulkQueryThreshold) {
            record(BULK_QUERY, resourceType, null, Map.of("resultCount", resultCount, "threshold", bulkQueryThreshold));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificationSent(UUID userId, String type) {
        record(NOTIFICATION_SENT, "users", userId, Map.of("type", type));
    }

    // ---------------------------------------------------------------- helpers

    private UUID currentCorrelationId() {
        String value = MDC.get("correlationId");
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        return truncate(clientIpResolver.resolve(request), 45);
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
