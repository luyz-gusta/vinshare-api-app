package com.fiap.vinshare.domain.dto.nps;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record NpsResponseDTO(
        UUID id,
        UUID serviceId,
        Short score,
        String comment,
        List<String> likedCategories,
        List<String> improvementCategories,
        OffsetDateTime createdAt
) {}
