package com.fiap.vinshare.domain.dto.auth;

import com.fiap.vinshare.domain.entities.UserRole;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AuthResponseDTO(
        UUID userId,
        String email,
        UserRole role,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {}
