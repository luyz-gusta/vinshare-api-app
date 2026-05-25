package com.fiap.vinshare.domain.dto.loyalty;

import lombok.Builder;

@Builder
public record LoyaltyBalanceDTO(
        int balance,
        int expiringIn30Days
) {}
