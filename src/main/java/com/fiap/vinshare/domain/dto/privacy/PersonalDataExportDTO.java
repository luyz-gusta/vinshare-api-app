package com.fiap.vinshare.domain.dto.privacy;

import com.fiap.vinshare.domain.dto.vehicle.VehicleResponseDTO;
import com.fiap.vinshare.domain.entities.AppointmentStatus;
import com.fiap.vinshare.domain.entities.UserRole;
import lombok.Builder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Cópia dos dados pessoais do titular (LGPD art. 18, II e V; art. 19). */
@Builder
public record PersonalDataExportDTO(
        OffsetDateTime generatedAt,
        Account account,
        Profile profile,
        List<VehicleResponseDTO> vehicles,
        List<AppointmentItem> appointments,
        List<NpsItem> npsResponses,
        int loyaltyBalance,
        long chatMessages,
        List<String> sharedWith
) {
    public record Account(UUID userId, String email, UserRole role, OffsetDateTime createdAt) {}

    public record Profile(String fullName, String cpf, String phone, LocalDate birthDate,
                          OffsetDateTime lgpdConsentAt) {}

    public record AppointmentItem(UUID id, String dealership, String serviceType,
                                  OffsetDateTime scheduledAt, AppointmentStatus status) {}

    public record NpsItem(UUID serviceId, Short score, String comment, OffsetDateTime createdAt) {}
}
