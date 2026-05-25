package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.Dealership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface DealershipRepository extends JpaRepository<Dealership, UUID> {

    /**
     * Busca concessionárias dentro do raio em km usando fórmula de Haversine.
     * Se serviceTypeId for nulo, ignora o filtro de tipo de serviço.
     */
    @Query(value = """
            SELECT d.*
              FROM dealerships d
              LEFT JOIN dealership_services ds
                ON ds.dealership_id = d.id
                AND (:serviceTypeId IS NULL OR ds.service_type_id = :serviceTypeId)
                AND ds.active = true
             WHERE (:serviceTypeId IS NULL OR ds.dealership_id IS NOT NULL)
               AND (6371 * acos(
                    cos(radians(:lat)) * cos(radians(d.lat)) *
                    cos(radians(d.lng) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(d.lat))
                  )) <= :radiusKm
             GROUP BY d.id
             ORDER BY (6371 * acos(
                    cos(radians(:lat)) * cos(radians(d.lat)) *
                    cos(radians(d.lng) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(d.lat))
                  )) ASC
             LIMIT 50
            """, nativeQuery = true)
    List<Dealership> findNearby(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng,
            @Param("radiusKm") double radiusKm,
            @Param("serviceTypeId") String serviceTypeId
    );
}
