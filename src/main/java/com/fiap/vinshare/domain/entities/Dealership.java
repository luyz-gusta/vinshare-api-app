package com.fiap.vinshare.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dealerships")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dealership {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "city", nullable = false, length = 120)
    private String city;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    @Column(name = "zip_code", length = 12)
    private String zipCode;

    @Column(name = "lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "opening_hours", length = 255)
    private String openingHours;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
