package com.fiap.vinshare.domain.dto.lead;

import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import com.fiap.vinshare.domain.entities.LeadHealthStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record LeadResponseDTO(
        UUID id,
        UUID customerId,
        String customerName,
        String cpfMasked,
        String vehicleModel,
        String vehiclePlate,
        LocalDate lastVisitAt,
        Integer daysSinceLastVisit,
        CustomerSegmentType segment,
        LeadHealthStatus status,
        BigDecimal riskScore,
        String warrantyStatus,
        Short lastNpsScore,
        String reason,
        String suggestedAction,
        OffsetDateTime updatedAt
) {}
