package com.fiap.vinshare.domain.dto.segment;

import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record CustomerSegmentDTO(
        UUID customerId,
        CustomerSegmentType segment,
        BigDecimal riskScore,
        Map<String, Object> topFeatures,
        String modelVersion,
        OffsetDateTime predictedAt
) {}
