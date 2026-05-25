package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.MaintenanceAlert;
import com.fiap.vinshare.domain.entities.ServiceRecord;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.domain.entities.Vehicle;
import com.fiap.vinshare.domain.entities.Warranty;
import com.fiap.vinshare.domain.entities.WarrantyStatus;
import com.fiap.vinshare.repositories.DeviceTokenRepository;
import com.fiap.vinshare.repositories.MaintenanceAlertRepository;
import com.fiap.vinshare.repositories.NpsResponseRepository;
import com.fiap.vinshare.repositories.ServiceRecordRepository;
import com.fiap.vinshare.repositories.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Jobs de notificação proativa (jornada do cliente, Desafio 2).
 * Ativado por `app.notifications.enabled=true`. Cada job busca os dispositivos
 * ativos dos clientes alvo e dispara push via Expo, registrando NOTIFICATION_SENT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true")
public class NotificationService {

    private static final int MAX_PER_RUN = 200;

    private final WarrantyRepository warrantyRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final MaintenanceAlertRepository maintenanceAlertRepository;
    private final NpsResponseRepository npsResponseRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final ExpoPushClient expoPushClient;
    private final AuditService auditService;

    /** Lembrete diário de revisão gratuita para garantias ativas. */
    @Scheduled(cron = "${app.notifications.cron.free-revision:0 0 9 * * *}")
    @Transactional
    public void freeRevisionReminder() {
        List<Warranty> active = warrantyRepository.findByStatus(WarrantyStatus.ACTIVE);
        int sent = 0;
        for (Warranty w : active.stream().limit(MAX_PER_RUN).toList()) {
            Vehicle vehicle = w.getVehicle();
            User user = vehicle.getCustomer().getUser();
            sent += notifyUser(user, "Revisão gratuita disponível",
                    "Sua " + vehicle.getModel() + " tem revisão gratuita enquanto a garantia estiver ativa. Agende agora!",
                    "FREE_REVISION", Map.of("vehicleId", vehicle.getId().toString()));
        }
        log.info("freeRevisionReminder: {} garantias ativas, {} notificações.", active.size(), sent);
    }

    /** Pesquisa NPS três dias após um serviço concluído sem avaliação. */
    @Scheduled(cron = "${app.notifications.cron.nps:0 0 * * * *}")
    @Transactional
    public void npsReminder() {
        OffsetDateTime end = OffsetDateTime.now().minusDays(3);
        OffsetDateTime start = end.minusDays(1);
        List<ServiceRecord> recent = serviceRecordRepository.findAllByPerformedAtBetween(start, end);
        int sent = 0;
        for (ServiceRecord record : recent.stream().limit(MAX_PER_RUN).toList()) {
            if (npsResponseRepository.existsByServiceId(record.getId())) continue;
            Customer customer = record.getVehicle().getCustomer();
            sent += notifyUser(customer.getUser(), "Como foi seu atendimento?",
                    "Avalie o serviço realizado na sua " + record.getVehicle().getModel() + ".",
                    "NPS_REQUEST", Map.of("serviceId", record.getId().toString()));
        }
        log.info("npsReminder: {} serviços na janela, {} notificações.", recent.size(), sent);
    }

    /** Lembrete de manutenção quando o veículo se aproxima do intervalo de revisão. */
    @Scheduled(cron = "${app.notifications.cron.km:0 30 9 * * *}")
    @Transactional
    public void maintenanceReminder() {
        List<MaintenanceAlert> due = maintenanceAlertRepository
                .findAllByDismissedAtIsNullAndDueDateBefore(LocalDate.now().plusDays(30));
        int sent = 0;
        for (MaintenanceAlert alert : due.stream().limit(MAX_PER_RUN).toList()) {
            Vehicle vehicle = alert.getVehicle();
            User user = vehicle.getCustomer().getUser();
            sent += notifyUser(user, "Manutenção se aproximando",
                    "Sua " + vehicle.getModel() + " está perto da próxima " + alert.getType() + ". Agende na concessionária.",
                    "MAINTENANCE_DUE", Map.of("vehicleId", vehicle.getId().toString()));
        }
        log.info("maintenanceReminder: {} alertas pendentes, {} notificações.", due.size(), sent);
    }

    private int notifyUser(User user, String title, String body, String type, Map<String, Object> data) {
        List<String> tokens = deviceTokenRepository.findAllByUserAndRevokedAtIsNull(user).stream()
                .map(d -> d.getToken())
                .toList();
        if (tokens.isEmpty()) return 0;
        int delivered = expoPushClient.send(tokens, title, body, data);
        if (delivered > 0) {
            auditService.notificationSent(user.getId(), type);
        }
        return delivered;
    }
}
