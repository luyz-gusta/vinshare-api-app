package com.fiap.vinshare.domain.dto.analytics;

import lombok.Builder;

import java.time.LocalDate;
import java.math.BigDecimal;

@Builder
public record VinShareSeriesPointDTO(
        LocalDate month,
        long vehiclesWithService,
        BigDecimal vinSharePercent
) {}
