package com.fiap.vinshare.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "lead_actions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadAction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analyst_id", nullable = false)
    private Analyst analyst;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::lead_channel")
    @Column(name = "channel", nullable = false, columnDefinition = "lead_channel")
    private LeadChannel channel;

    @Column(name = "template_id", length = 80)
    private String templateId;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::lead_status")
    @Column(name = "status", nullable = false, columnDefinition = "lead_status")
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
