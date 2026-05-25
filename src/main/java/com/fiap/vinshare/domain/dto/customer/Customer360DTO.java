package com.fiap.vinshare.domain.dto.customer;

import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import com.fiap.vinshare.domain.entities.WarrantyStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record Customer360DTO(
        CustomerInfo customer,
        VehicleInfo vehicle,
        SegmentInfo segment,
        LifetimeStats lifetimeStats
) {
    @Builder
    public record CustomerInfo(
            UUID id,
            String name,
            String cpfMasked,
            String email,
            String phone
    ) {}

    @Builder
    public record VehicleInfo(
            UUID id,
            String model,
            Integer year,
            Integer currentKm,
            WarrantyStatus warrantyStatus
    ) {}

    @Builder
    public record SegmentInfo(
            CustomerSegmentType name,
            BigDecimal riskScore
    ) {}

    @Builder
    public record LifetimeStats(
            long servicesCount,
            BigDecimal totalSpent,
            BigDecimal averageNps
    ) {}
}
