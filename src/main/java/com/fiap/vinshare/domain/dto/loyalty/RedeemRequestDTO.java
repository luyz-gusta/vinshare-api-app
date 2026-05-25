package com.fiap.vinshare.domain.dto.loyalty;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RedeemRequestDTO(
        @NotNull UUID rewardId
) {}
