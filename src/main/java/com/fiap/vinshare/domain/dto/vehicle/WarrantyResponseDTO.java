package com.fiap.vinshare.domain.dto.vehicle;

import com.fiap.vinshare.domain.entities.WarrantyStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record WarrantyResponseDTO(
        UUID vehicleId,
        WarrantyStatus status,
        LocalDate startDate,
        LocalDate endDate,
        long daysRemaining,
        boolean freeRevisionAvailable
) {}
