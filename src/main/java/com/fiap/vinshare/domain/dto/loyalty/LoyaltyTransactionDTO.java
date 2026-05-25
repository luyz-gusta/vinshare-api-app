package com.fiap.vinshare.domain.dto.loyalty;

import com.fiap.vinshare.domain.entities.LoyaltyTransactionType;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record LoyaltyTransactionDTO(
        UUID id,
        LoyaltyTransactionType type,
        int points,
        UUID sourceServiceId,
        UUID sourceRewardId,
        String rewardName,
        String voucherCode,
        OffsetDateTime createdAt
) {}
