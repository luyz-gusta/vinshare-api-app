package com.fiap.vinshare.domain.dto.chat;

import lombok.Builder;

@Builder
public record SuggestedActionDTO(
        String type,
        String label,
        String target
) {}
