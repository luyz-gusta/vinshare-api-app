package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.NpsResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NpsResponseRepository extends JpaRepository<NpsResponse, UUID> {

    Optional<NpsResponse> findByServiceId(UUID serviceId);

    boolean existsByServiceId(UUID serviceId);

    /**
     * Lista serviços do cliente concluídos que ainda não receberam NPS.
     * Retorna IDs de serviço.
     */
    @Query(value = """
            SELECT s.id FROM services s
              JOIN vehicles v ON v.id = s.vehicle_id
             WHERE v.customer_id = :customerId
               AND NOT EXISTS (
                   SELECT 1 FROM nps_responses n WHERE n.service_id = s.id
               )
             ORDER BY s.performed_at DESC
            """, nativeQuery = true)
    List<UUID> findPendingServiceIdsForCustomer(@Param("customerId") UUID customerId);

    /**
     * Agregação para o dashboard analítico:
     * total, média de score, e contagens nas três faixas (promotor/passivo/detrator).
     */
    @Query(value = """
            SELECT COUNT(*)::bigint                                       AS total,
                   COALESCE(AVG(score), 0)::numeric                       AS avg_score,
                   COUNT(*) FILTER (WHERE score >= 9)::bigint             AS promoters,
                   COUNT(*) FILTER (WHERE score BETWEEN 7 AND 8)::bigint  AS passives,
                   COUNT(*) FILTER (WHERE score <= 6)::bigint             AS detractors
              FROM nps_responses
             WHERE (CAST(:from AS timestamptz) IS NULL OR created_at >= :from)
            """, nativeQuery = true)
    Object summary(@Param("from") OffsetDateTime from);
}
