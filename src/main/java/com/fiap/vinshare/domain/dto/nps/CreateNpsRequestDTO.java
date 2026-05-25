package com.fiap.vinshare.domain.dto.nps;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateNpsRequestDTO(
        @NotNull @Min(0) @Max(10) Short score,
        @Size(max = 1000) String comment,
        List<@Size(max = 40) String> likedCategories,
        List<@Size(max = 40) String> improvementCategories
) {}
