package com.fiap.vinshare.domain.dto.vehicle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateOdometerRequestDTO(
        @NotNull @Min(0) @Max(2_000_000) Integer km
) {}
