package com.fiap.vinshare.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "nps_responses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NpsResponse {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, unique = true)
    private ServiceRecord service;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "score", nullable = false)
    private Short score;

    @Column(name = "comment", length = 1000)
    private String comment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "liked_categories", columnDefinition = "jsonb")
    private List<String> likedCategories;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "improvement_categories", columnDefinition = "jsonb")
    private List<String> improvementCategories;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
