package com.fiap.vinshare.domain.dto.dealership;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalTime;

@Builder
public record AvailabilitySlotDTO(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime time,
        boolean available
) {}
