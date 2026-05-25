package com.fiap.vinshare.domain.dto.lead;

import com.fiap.vinshare.domain.entities.LeadChannel;
import com.fiap.vinshare.domain.entities.LeadStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record LeadActionResponseDTO(
        UUID id,
        UUID customerId,
        UUID analystId,
        LeadChannel channel,
        String templateId,
        LeadStatus status,
        boolean simulated,
        OffsetDateTime createdAt
) {}
