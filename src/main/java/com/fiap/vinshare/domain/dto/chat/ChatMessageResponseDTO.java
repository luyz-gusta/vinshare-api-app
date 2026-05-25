package com.fiap.vinshare.domain.dto.chat;

import com.fiap.vinshare.domain.entities.ChatMessageRole;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record ChatMessageResponseDTO(
        UUID id,
        ChatMessageRole role,
        String content,
        List<SuggestedActionDTO> suggestedActions,
        OffsetDateTime createdAt
) {}
