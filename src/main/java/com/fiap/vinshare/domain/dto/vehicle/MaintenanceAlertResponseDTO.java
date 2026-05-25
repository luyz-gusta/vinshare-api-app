package com.fiap.vinshare.domain.dto.vehicle;

import com.fiap.vinshare.domain.entities.MaintenanceAlertType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record MaintenanceAlertResponseDTO(
        UUID id,
        MaintenanceAlertType type,
        String title,
        Integer kmThreshold,
        Integer currentKm,
        Integer kmRemaining,
        LocalDate dueDate
) {}
