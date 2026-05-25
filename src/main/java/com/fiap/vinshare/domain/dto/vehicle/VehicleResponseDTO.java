package com.fiap.vinshare.domain.dto.vehicle;

import com.fiap.vinshare.domain.entities.WarrantyStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record VehicleResponseDTO(
        UUID id,
        String model,
        String version,
        Integer year,
        String plate,
        String vin,
        Integer currentKm,
        WarrantyStatus warrantyStatus
) {}
