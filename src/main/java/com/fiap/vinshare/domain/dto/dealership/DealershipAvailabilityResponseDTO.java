package com.fiap.vinshare.domain.dto.dealership;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record DealershipAvailabilityResponseDTO(
        LocalDate date,
        List<AvailabilitySlotDTO> slots
) {}
