package com.fiap.vinshare.domain.dto.loyalty;

import lombok.Builder;

import java.util.UUID;

@Builder
public record RewardResponseDTO(
        UUID id,
        String name,
        String description,
        int pointsCost
) {}
