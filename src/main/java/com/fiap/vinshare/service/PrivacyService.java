package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.privacy.PersonalDataExportDTO;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.DeviceToken;
import com.fiap.vinshare.domain.entities.LoyaltyAccount;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.infra.security.CryptoService;
import com.fiap.vinshare.repositories.AppointmentRepository;
import com.fiap.vinshare.repositories.ChatMessageRepository;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.DeviceTokenRepository;
import com.fiap.vinshare.repositories.LoyaltyAccountRepository;
import com.fiap.vinshare.repositories.NpsResponseRepository;
import com.fiap.vinshare.repositories.RefreshTokenRepository;
import com.fiap.vinshare.repositories.UserRepository;
import com.fiap.vinshare.repositories.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Direitos do titular (LGPD art. 18): acesso/portabilidade e eliminação por anonimização. */
@Service
@RequiredArgsConstructor
public class PrivacyService {

    /** Operadores e terceiros que recebem dados (LGPD art. 18, VII). */
    private static final List<String> SHARED_WITH = List.of(
            "Microsoft Azure (EUA): hospedagem da API e logs de aplicação",
            "Neon: banco de dados PostgreSQL",
            "Google Gemini: mensagens do chat, com CPF, e-mail e telefone removidos antes do envio",
            "Expo: token de notificação push do dispositivo");

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final AppointmentRepository appointmentRepository;
    private final NpsResponseRepository npsRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final VehicleService vehicleService;
    private final CryptoService cryptoService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PersonalDataExportDTO export(User user) {
        Customer customer = requireCustomer(user);

        List<PersonalDataExportDTO.AppointmentItem> appointments = appointmentRepository
                .findAllByCustomerIdOrderByScheduledAtDesc(customer.getId(), Pageable.unpaged()).stream()
                .map(a -> new PersonalDataExportDTO.AppointmentItem(a.getId(), a.getDealership().getName(),
                        a.getServiceType().getLabel(), a.getScheduledAt(), a.getStatus()))
                .toList();
        List<PersonalDataExportDTO.NpsItem> nps = npsRepository.findAllByCustomerId(customer.getId()).stream()
                .map(n -> new PersonalDataExportDTO.NpsItem(n.getService().getId(), n.getScore(),
                        n.getComment(), n.getCreatedAt()))
                .toList();
        int balance = loyaltyAccountRepository.findByCustomerId(customer.getId())
                .map(LoyaltyAccount::getBalance)
                .orElse(0);

        auditService.record(AuditService.DATA_EXPORTED, "customers", customer.getId(), Map.of());

        return PersonalDataExportDTO.builder()
                .generatedAt(OffsetDateTime.now())
                .account(new PersonalDataExportDTO.Account(user.getId(), user.getEmail(), user.getRole(),
                        user.getCreatedAt()))
                .profile(new PersonalDataExportDTO.Profile(customer.getFullName(),
                        cryptoService.decrypt(customer.getCpfEncrypted()), customer.getPhone(),
                        customer.getBirthDate(), customer.getLgpdConsentAt()))
                .vehicles(vehicleService.listOwnedBy(user))
                .appointments(appointments)
                .npsResponses(nps)
                .loyaltyBalance(balance)
                .chatMessages(chatMessageRepository.countBySession_User_Id(user.getId()))
                .sharedWith(SHARED_WITH)
                .build();
    }

    /**
     * Elimina os identificadores diretos do titular e encerra a conta.
     * Serviços, valores e pontos permanecem, sem vínculo com a pessoa, por
     * obrigação legal e contábil (LGPD art. 16, I) e para as estatísticas de VIN Share.
     */
    @Transactional
    public void anonymize(User user) {
        Customer customer = requireCustomer(user);
        String anonId = UUID.randomUUID().toString();

        user.setEmail("anonimizado+" + anonId + "@vinshare.invalid");
        user.setDisplayName(null);
        user.setActive(false);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        userRepository.save(user);

        customer.setFullName("Titular anonimizado");
        customer.setPhone(null);
        customer.setBirthDate(null);
        customer.setCpfEncrypted(cryptoService.encrypt("00000000000"));
        customer.setCpfLookupHash(cryptoService.hash("anonimizado:" + anonId));
        customerRepository.save(customer);

        vehicleRepository.findAllByCustomerId(customer.getId()).forEach(v -> {
            v.setPlate(null);
            vehicleRepository.save(v);
        });
        npsRepository.clearCommentsByCustomerId(customer.getId());
        chatMessageRepository.deleteAllByUserId(user.getId());
        refreshTokenRepository.deleteAllByUser(user);   // T13.1: apagar evita alarme falso de reuso
        deviceTokenRepository.findAllByUserAndRevokedAtIsNull(user).forEach(DeviceToken::revoke);

        auditService.record(AuditService.DATA_SUBJECT_ANONYMIZED, "customers", customer.getId(), Map.of());
    }

    private Customer requireCustomer(User user) {
        return customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o usuário logado"));
    }
}
