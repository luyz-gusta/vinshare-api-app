package com.fiap.vinshare.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTransaction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private LoyaltyAccount account;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::loyalty_transaction_type")
    @Column(name = "type", nullable = false, columnDefinition = "loyalty_transaction_type")
    private LoyaltyTransactionType type;

    @Column(name = "points", nullable = false)
    private Integer points;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_service_id")
    private ServiceRecord sourceService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_reward_id")
    private Reward sourceReward;

    @Column(name = "voucher_code", length = 40)
    private String voucherCode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
