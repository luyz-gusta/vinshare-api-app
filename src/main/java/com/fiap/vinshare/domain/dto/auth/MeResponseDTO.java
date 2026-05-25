package com.fiap.vinshare.domain.dto.auth;

import com.fiap.vinshare.domain.entities.UserRole;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record MeResponseDTO(
        UUID userId,
        UserRole role,
        String email,
        String fullName,
        String phone,
        OffsetDateTime createdAt
) {}
