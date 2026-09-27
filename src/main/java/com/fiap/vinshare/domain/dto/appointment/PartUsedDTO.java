package com.fiap.vinshare.domain.dto.appointment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PartUsedDTO(
        @NotBlank @Size(max = 180) String partName,
        @NotNull @Min(1) @Max(100) Integer quantity,
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "50000.00") BigDecimal unitPrice
) {}
