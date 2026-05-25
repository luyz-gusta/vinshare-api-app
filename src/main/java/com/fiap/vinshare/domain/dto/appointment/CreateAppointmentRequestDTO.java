package com.fiap.vinshare.domain.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateAppointmentRequestDTO(
        @NotNull UUID vehicleId,
        @NotNull UUID dealershipId,
        @NotBlank @Size(max = 40) String serviceTypeId,
        @NotNull OffsetDateTime scheduledAt,
        @Size(max = 500) String notes
) {}
