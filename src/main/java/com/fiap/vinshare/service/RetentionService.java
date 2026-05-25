package com.fiap.vinshare.service;

import com.fiap.vinshare.repositories.AuditLogRepository;
import com.fiap.vinshare.repositories.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Política de retenção de dados (Cyber, frente 4).
 *
 * Roda diariamente e remove registros mais antigos que a janela configurada.
 * Default: 90 dias para chat e 180 dias para audit log (auditoria precisa de
 * janela maior por requisito regulatório).
 *
 * Ativado por `app.retention.enabled=true`. Desligado por padrão para evitar
 * deletar dados em ambientes de desenvolvimento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.retention.enabled", havingValue = "true")
public class RetentionService {

    private final ChatMessageRepository chatMessageRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${app.retention.chat-days:90}")
    private int chatDays;

    @Value("${app.retention.audit-days:180}")
    private int auditDays;

    /**
     * Job diário às 03:30 (horário Brasília via timezone do Hibernate).
     * Configurável por `app.retention.cron`.
     */
    @Scheduled(cron = "${app.retention.cron:0 30 3 * * *}")
    @Transactional
    public void purgeExpiredRecords() {
        OffsetDateTime chatCutoff = OffsetDateTime.now().minusDays(chatDays);
        OffsetDateTime auditCutoff = OffsetDateTime.now().minusDays(auditDays);

        int chatRemoved = chatMessageRepository.deleteOlderThan(chatCutoff);
        int auditRemoved = auditLogRepository.deleteOlderThan(auditCutoff);

        log.info("Política de retenção aplicada: chat_messages removidas={} (>{}d), audit_log removidos={} (>{}d)",
                chatRemoved, chatDays, auditRemoved, auditDays);
    }
}
