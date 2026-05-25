package com.fiap.vinshare.domain.dto.segment;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record SegmentDistributionResponseDTO(
        long totalCustomers,
        List<SegmentBucketDTO> buckets,
        OffsetDateTime computedAt
) {}
