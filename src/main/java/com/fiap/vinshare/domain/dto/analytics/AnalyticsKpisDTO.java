package com.fiap.vinshare.domain.dto.analytics;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AnalyticsKpisDTO(
        long vehiclesUnderWarranty,
        BigDecimal vinSharePercent,
        BigDecimal estimatedRevenue,
        long leadsAtRisk
) {}
