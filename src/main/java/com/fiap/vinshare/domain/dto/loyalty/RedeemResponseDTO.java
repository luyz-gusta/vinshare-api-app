package com.fiap.vinshare.domain.dto.loyalty;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record RedeemResponseDTO(
        UUID transactionId,
        UUID rewardId,
        String rewardName,
        String voucherCode,
        OffsetDateTime expiresAt,
        int pointsSpent,
        int newBalance
) {}
