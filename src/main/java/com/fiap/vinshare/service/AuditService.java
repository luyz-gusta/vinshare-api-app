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
 *  - Grava na transação do chamador: o registro e a operação são confirmados juntos,
 *    e cada requisição usa uma única conexão do pool. Uma transação própria
 *    (REQUIRES_NEW) pediria uma segunda conexão com a primeira presa, e sob carga o
 *    pool trava (todas as threads esperando a segunda conexão).
 *  - Eventos de falha registrados antes de uma exceção só persistem se o chamador
 *    declarar noRollbackFor para ela (login, refresh, troca de senha, exclusão de conta).
 *  - Erros ao montar o registro não derrubam a requisição; erro do banco derruba,
 *    porque o registro faz parte da transação da operação.
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
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String LOYALTY_REDEEM = "LOYALTY_REDEEM";
    public static final String SERVICE_COMPLETED = "SERVICE_COMPLETED";
    public static final String ODOMETER_UPDATED = "ODOMETER_UPDATED";
    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String PII_ACCESS = "PII_ACCESS";
    public static final String DATA_EXPORTED = "DATA_EXPORTED";
    public static final String DATA_SUBJECT_ANONYMIZED = "DATA_SUBJECT_ANONYMIZED";

    private final AuditLogRepository auditLogRepository;
    private final ClientIpResolver clientIpResolver;

    @Value("${security.audit.bulk-query-threshold:100}")
    private int bulkQueryThreshold;

    @Transactional
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

    @Transactional
    public void loginSuccess(UUID userId, String email) {
        record(LOGIN_SUCCESS, "users", userId, Map.of("email", email));
    }

    @Transactional
    public void loginFailure(String email) {
        record(LOGIN_FAILURE, "users", null, Map.of("email", email == null ? "" : email));
    }

    @Transactional
    public void suspiciousLogin(String email, int attempts) {
        record(SUSPICIOUS_LOGIN, "users", null, Map.of("email", email == null ? "" : email, "attempts", attempts));
    }

    @Transactional
    public void leadAction(UUID customerId, String channel, String templateId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channel);
        if (templateId != null) payload.put("templateId", templateId);
        record(LEAD_ACTION, "customers", customerId, payload);
    }

    /**
     * Registra BULK_QUERY quando uma listagem retorna acima do threshold configurado.
     */
    @Transactional
    public void checkBulkQuery(String resourceType, long resultCount) {
        if (resultCount >= bulkQueryThreshold) {
            record(BULK_QUERY, resourceType, null, Map.of("resultCount", resultCount, "threshold", bulkQueryThreshold));
        }
    }

    @Transactional
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
