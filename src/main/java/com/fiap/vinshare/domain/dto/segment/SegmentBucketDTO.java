package com.fiap.vinshare.domain.dto.segment;

import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record SegmentBucketDTO(
        CustomerSegmentType segment,
        long count,
        BigDecimal percent,
        BigDecimal avgTicket,
        BigDecimal avgNps
) {}
