package com.fiap.vinshare.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Tabela de associação N:N entre Dealership e ServiceType,
 * com flag adicional `active` indicando se o serviço está vigente
 * naquela concessionária.
 */
@Entity
@Table(name = "dealership_services")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealershipServiceLink {

    @EmbeddedId
    private Id pk;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("dealershipId")
    @JoinColumn(name = "dealership_id", nullable = false)
    private Dealership dealership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("serviceTypeId")
    @JoinColumn(name = "service_type_id", nullable = false)
    private ServiceType serviceType;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Embeddable
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "dealership_id")
        private UUID dealershipId;

        @Column(name = "service_type_id", length = 40)
        private String serviceTypeId;
    }
}
