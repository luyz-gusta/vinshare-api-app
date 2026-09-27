package com.fiap.vinshare.domain.dto.appointment;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CompleteAppointmentRequestDTO(
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "50000.00") BigDecimal totalAmount,
        @Size(max = 500) String summary,
        @Valid @Size(max = 50) List<PartUsedDTO> partsUsed
) {}
