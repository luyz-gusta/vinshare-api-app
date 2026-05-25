package com.fiap.vinshare.domain.dto.analytics;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Resumo de NPS para o dashboard analítico.
 *  - averageScore: média simples dos scores 0..10
 *  - npsScore: (promotores% - detratores%), no formato canônico de -100 a 100
 *  - promoters/passives/detractors: contagens absolutas
 */
@Builder
public record NpsSummaryDTO(
        long totalResponses,
        BigDecimal averageScore,
        BigDecimal npsScore,
        long promoters,
        long passives,
        long detractors,
        OffsetDateTime computedAt
) {}
