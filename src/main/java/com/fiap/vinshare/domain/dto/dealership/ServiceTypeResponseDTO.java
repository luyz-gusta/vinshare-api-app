package com.fiap.vinshare.domain.dto.dealership;

import lombok.Builder;

@Builder
public record ServiceTypeResponseDTO(
        String id,
        String label,
        boolean freeWithWarranty
) {}
