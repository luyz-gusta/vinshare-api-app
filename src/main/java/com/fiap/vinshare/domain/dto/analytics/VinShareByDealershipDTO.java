package com.fiap.vinshare.domain.dto.analytics;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record VinShareByDealershipDTO(
        UUID dealershipId,
        String name,
        long vehiclesServed,
        long vehiclesTotal,
        BigDecimal sharePercent,
        BigDecimal estimatedRevenue,
        Trend trend
) {
    public enum Trend { UP, DOWN, FLAT }
}
