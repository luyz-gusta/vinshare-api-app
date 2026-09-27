package com.fiap.vinshare.domain.dto.loyalty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RewardRequestDTO(
        @NotBlank @Size(max = 180) String name,
        @Size(max = 500) String description,
        @NotNull @Min(1) @Max(1_000_000) Integer pointsCost,
        Boolean active
) {}
