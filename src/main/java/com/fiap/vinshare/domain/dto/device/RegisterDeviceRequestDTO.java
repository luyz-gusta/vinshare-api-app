package com.fiap.vinshare.domain.dto.device;

import com.fiap.vinshare.domain.entities.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequestDTO(
        @NotBlank @Size(max = 255) String token,
        @NotNull DevicePlatform platform
) {}
