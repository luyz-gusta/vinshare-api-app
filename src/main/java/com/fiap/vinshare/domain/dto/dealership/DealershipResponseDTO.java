package com.fiap.vinshare.domain.dto.dealership;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record DealershipResponseDTO(
        UUID id,
        String name,
        String address,
        String city,
        String state,
        String phone,
        BigDecimal lat,
        BigDecimal lng,
        Double distanceKm,
        String openingHours
) {}
