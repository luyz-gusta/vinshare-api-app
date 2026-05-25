package com.fiap.vinshare.domain.dto.device;

import com.fiap.vinshare.domain.entities.DevicePlatform;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record DeviceResponseDTO(
        UUID id,
        String token,
        DevicePlatform platform,
        OffsetDateTime createdAt
) {}
