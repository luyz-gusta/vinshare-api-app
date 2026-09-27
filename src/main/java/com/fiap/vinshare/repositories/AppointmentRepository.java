package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Page<Appointment> findAllByCustomerIdOrderByScheduledAtDesc(UUID customerId, Pageable pageable);

    /**
     * {@code status} é comparado como texto (veja {@link #existsActiveConflict}
     * logo abaixo pro motivo) — por isso recebe {@code AppointmentStatus.name()},
     * não o enum.
     */
    @Query("""
            select a from Appointment a
            where a.customer.id = :customerId
              and str(a.status) = :status
            order by a.scheduledAt desc
            """)
    Page<Appointment> findAllByCustomerIdAndStatus(
            @Param("customerId") UUID customerId,
            @Param("status") String status,
            Pageable pageable);

    Optional<Appointment> findByIdAndCustomerId(UUID id, UUID customerId);

    /**
     * Comparação por texto, não pelo enum {@code AppointmentStatus} diretamente:
     * mesmo com {@code @JdbcType(PostgreSQLEnumJdbcType.class)} na coluna,
     * confirmei em produção que a escrita de uma entidade gerenciada (INSERT/UPDATE
     * via {@code save()}) funciona bem com o enum nativo — mas o bind de um enum
     * como *parâmetro de query* continua quebrando em runtime, tanto em query
     * derivada quanto em JPQL explícita com o enum tipado. `str(a.status)` compara
     * como texto dos dois lados, contornando o bind do enum sem abrir mão do tipo
     * nativo na coluna.
     */
    @Query("""
            select case when count(a) > 0 then true else false end
            from Appointment a
            where a.dealership.id = :dealershipId
              and a.scheduledAt = :scheduledAt
              and (str(a.status) = :first or str(a.status) = :second)
            """)
    boolean existsActiveConflict(
            @Param("dealershipId") UUID dealershipId,
            @Param("scheduledAt") OffsetDateTime scheduledAt,
            @Param("first") String first,
            @Param("second") String second);
}
