package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.CustomerSegment;
import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, UUID> {

    /** Retorna a predição mais recente de um cliente. */
    Optional<CustomerSegment> findFirstByCustomerIdOrderByPredictedAtDesc(UUID customerId);

    /**
     * Última predição de cada cliente, filtrada por segmentos, faixa de risco e
     * (quando allDealerships = false) pela concessionária de relacionamento.
     * Ordena do maior risco para o menor.
     */
    @Query(value = """
            SELECT cs.* FROM customer_segments cs
              JOIN customers c ON c.id = cs.customer_id
             WHERE cs.predicted_at = (
                   SELECT MAX(cs2.predicted_at) FROM customer_segments cs2
                    WHERE cs2.customer_id = cs.customer_id
             )
               AND CAST(cs.segment AS text) IN (:segments)
               AND cs.risk_score >= :minRisk AND cs.risk_score < :maxRisk
               AND (:allDealerships = true OR c.home_dealership_id = CAST(:dealershipId AS uuid))
             ORDER BY cs.risk_score DESC, cs.customer_id
            """,
            countQuery = """
            SELECT COUNT(*) FROM customer_segments cs
              JOIN customers c ON c.id = cs.customer_id
             WHERE cs.predicted_at = (
                   SELECT MAX(cs2.predicted_at) FROM customer_segments cs2
                    WHERE cs2.customer_id = cs.customer_id
             )
               AND CAST(cs.segment AS text) IN (:segments)
               AND cs.risk_score >= :minRisk AND cs.risk_score < :maxRisk
               AND (:allDealerships = true OR c.home_dealership_id = CAST(:dealershipId AS uuid))
            """,
            nativeQuery = true)
    Page<CustomerSegment> findLatestScoped(@Param("segments") java.util.Collection<String> segments,
                                           @Param("minRisk") java.math.BigDecimal minRisk,
                                           @Param("maxRisk") java.math.BigDecimal maxRisk,
                                           @Param("allDealerships") boolean allDealerships,
                                           @Param("dealershipId") String dealershipId,
                                           Pageable pageable);

    /** Conta por segmento na predição mais recente de cada cliente. */
    @Query(value = """
            SELECT cs.segment AS segment, COUNT(*) AS total
              FROM customer_segments cs
             WHERE cs.predicted_at = (
                   SELECT MAX(cs2.predicted_at) FROM customer_segments cs2
                    WHERE cs2.customer_id = cs.customer_id
             )
             GROUP BY cs.segment
            """, nativeQuery = true)
    java.util.List<Object[]> countLatestBySegmentRaw();

    default java.util.Map<CustomerSegmentType, Long> countLatestBySegment() {
        java.util.Map<CustomerSegmentType, Long> map = new java.util.EnumMap<>(CustomerSegmentType.class);
        for (CustomerSegmentType t : CustomerSegmentType.values()) map.put(t, 0L);
        for (Object[] row : countLatestBySegmentRaw()) {
            CustomerSegmentType type = CustomerSegmentType.valueOf((String) row[0]);
            map.put(type, ((Number) row[1]).longValue());
        }
        return map;
    }

    /**
     * Distribuição agregada: por segmento da última predição de cada cliente,
     * traz count, ticket médio dos serviços do cliente e nota média de NPS.
     */
    @Query(value = """
            WITH latest AS (
              SELECT DISTINCT ON (cs.customer_id) cs.customer_id, cs.segment
                FROM customer_segments cs
               ORDER BY cs.customer_id, cs.predicted_at DESC
            )
            SELECT CAST(l.segment AS text)              AS segment,
                   COUNT(*)::bigint                     AS cnt,
                   COALESCE(AVG(s.total_amount), 0)::numeric AS avg_ticket,
                   COALESCE(AVG(n.score), 0)::numeric        AS avg_nps
              FROM latest l
              LEFT JOIN vehicles v        ON v.customer_id = l.customer_id
              LEFT JOIN services s        ON s.vehicle_id  = v.id
              LEFT JOIN nps_responses n   ON n.customer_id = l.customer_id
             GROUP BY l.segment
            """, nativeQuery = true)
    java.util.List<Object[]> distributionAggregated();
}
