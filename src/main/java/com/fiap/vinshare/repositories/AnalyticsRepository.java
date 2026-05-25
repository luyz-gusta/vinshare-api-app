package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Queries agregadas para os endpoints de analytics do analista.
 * O repositório se ancora em ServiceRecord apenas para ter um tipo gerenciado,
 * mas as queries são todas nativas e podem cruzar várias tabelas.
 */
@Repository
public interface AnalyticsRepository extends JpaRepository<ServiceRecord, UUID> {

    @Query(value = """
            WITH warranty_universe AS (
              SELECT v.id, v.customer_id
                FROM vehicles v
                JOIN warranties w ON w.vehicle_id = v.id
               WHERE w.status IN ('ACTIVE','EXPIRING_SOON')
            )
            SELECT
              (SELECT COUNT(*) FROM warranty_universe) AS vehiclesUnderWarranty,
              COALESCE((
                SELECT ROUND( (COUNT(DISTINCT s.vehicle_id)::numeric * 100.0 /
                               NULLIF((SELECT COUNT(*) FROM warranty_universe),0)), 1)
                  FROM services s
                  JOIN warranty_universe wu ON wu.id = s.vehicle_id
                 WHERE s.performed_at >= :from
              ),0) AS vinSharePercent,
              COALESCE((SELECT SUM(s.total_amount) FROM services s WHERE s.performed_at >= :from),0)
                 AS estimatedRevenue
            """, nativeQuery = true)
    Object[] computeKpis(@Param("from") OffsetDateTime from);

    @Query(value = """
            SELECT date_trunc('month', s.performed_at) AS bucket,
                   COUNT(DISTINCT s.vehicle_id)         AS vehicles_with_service
              FROM services s
             WHERE s.performed_at BETWEEN :from AND :to
             GROUP BY bucket
             ORDER BY bucket ASC
            """, nativeQuery = true)
    List<Object[]> seriesByMonth(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
            WITH curr AS (
              SELECT d.id, d.name,
                     COUNT(DISTINCT v.id) FILTER (
                       WHERE EXISTS (
                         SELECT 1 FROM services s
                          WHERE s.vehicle_id = v.id
                            AND s.dealership_id = d.id
                            AND s.performed_at >= :from
                       )
                     ) AS vehicles_served,
                     COUNT(DISTINCT v.id) AS vehicles_total,
                     COALESCE((SELECT SUM(s.total_amount) FROM services s
                                WHERE s.dealership_id = d.id
                                  AND s.performed_at >= :from), 0) AS revenue
                FROM dealerships d
                LEFT JOIN services s ON s.dealership_id = d.id
                LEFT JOIN vehicles v ON v.id = s.vehicle_id
               GROUP BY d.id, d.name
            ),
            prev AS (
              SELECT d.id,
                     COUNT(DISTINCT s.vehicle_id) AS vehicles_served_prev
                FROM dealerships d
                LEFT JOIN services s ON s.dealership_id = d.id
                 AND s.performed_at >= :prevFrom
                 AND s.performed_at <  :from
               GROUP BY d.id
            )
            SELECT c.id, c.name, c.vehicles_served, c.vehicles_total, c.revenue,
                   COALESCE(p.vehicles_served_prev, 0) AS vehicles_served_prev
              FROM curr c
              LEFT JOIN prev p ON p.id = c.id
             ORDER BY c.vehicles_served DESC NULLS LAST
            """, nativeQuery = true)
    List<Object[]> shareByDealership(@Param("from") OffsetDateTime from,
                                     @Param("prevFrom") OffsetDateTime prevFrom);
}
