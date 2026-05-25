package com.fiap.vinshare.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_types")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceType {

    @Id
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "free_with_warranty", nullable = false)
    @Builder.Default
    private boolean freeWithWarranty = false;
}
