package com.fiap.vinshare.domain.dto.lead;

import com.fiap.vinshare.domain.entities.LeadChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeadActionRequestDTO(
        @NotNull LeadChannel channel,
        @Size(max = 80) String templateId,
        @Size(max = 500) String notes
) {}
