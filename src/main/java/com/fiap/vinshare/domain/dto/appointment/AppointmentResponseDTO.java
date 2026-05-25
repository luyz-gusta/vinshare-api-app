package com.fiap.vinshare.domain.dto.appointment;

import com.fiap.vinshare.domain.entities.AppointmentStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record AppointmentResponseDTO(
        UUID id,
        UUID vehicleId,
        String vehicleModel,
        UUID dealershipId,
        String dealershipName,
        String serviceTypeId,
        String serviceTypeLabel,
        OffsetDateTime scheduledAt,
        AppointmentStatus status,
        String notes,
        OffsetDateTime createdAt
) {}
