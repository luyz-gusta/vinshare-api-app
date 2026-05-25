package com.fiap.vinshare.domain.dto.nps;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record PendingSurveyDTO(
        UUID serviceId,
        String dealershipName,
        String serviceTypeLabel,
        OffsetDateTime performedAt
) {}
