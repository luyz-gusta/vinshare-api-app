package com.fiap.vinshare.domain.dto.chat;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record ChatSessionResponseDTO(
        UUID sessionId,
        OffsetDateTime startedAt
) {}
