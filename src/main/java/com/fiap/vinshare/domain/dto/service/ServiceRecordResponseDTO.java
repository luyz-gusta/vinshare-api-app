package com.fiap.vinshare.domain.dto.service;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record ServiceRecordResponseDTO(
        UUID id,
        UUID vehicleId,
        String vehicleModel,
        UUID dealershipId,
        String dealershipName,
        String serviceTypeId,
        String serviceTypeLabel,
        OffsetDateTime performedAt,
        BigDecimal totalAmount,
        String summary
) {}
