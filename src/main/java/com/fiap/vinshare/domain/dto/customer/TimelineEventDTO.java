package com.fiap.vinshare.domain.dto.customer;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record TimelineEventDTO(
        OffsetDateTime at,
        String type,
        String title,
        String description
) {}
