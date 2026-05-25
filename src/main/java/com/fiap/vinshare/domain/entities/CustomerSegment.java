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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "customer_segments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSegment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::customer_segment")
    @Column(name = "segment", nullable = false, columnDefinition = "customer_segment")
    private CustomerSegmentType segment;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_features", columnDefinition = "jsonb")
    private Map<String, Object> topFeatures;

    @Column(name = "model_version", nullable = false, length = 40)
    private String modelVersion;

    @Column(name = "predicted_at", nullable = false)
    private OffsetDateTime predictedAt;

    @PrePersist
    void onCreate() {
        if (predictedAt == null) predictedAt = OffsetDateTime.now();
    }
}
