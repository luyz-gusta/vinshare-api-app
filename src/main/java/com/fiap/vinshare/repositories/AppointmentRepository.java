package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.Appointment;
import com.fiap.vinshare.domain.entities.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Page<Appointment> findAllByCustomerIdOrderByScheduledAtDesc(UUID customerId, Pageable pageable);

    Page<Appointment> findAllByCustomerIdAndStatusOrderByScheduledAtDesc(
            UUID customerId, AppointmentStatus status, Pageable pageable);

    Optional<Appointment> findByIdAndCustomerId(UUID id, UUID customerId);

    List<Appointment> findAllByDealershipIdAndScheduledAtBetweenAndStatusIn(
            UUID dealershipId, OffsetDateTime from, OffsetDateTime to, List<AppointmentStatus> statuses);

    /**
     * Escrita explícita em vez de {@code existsBy...StatusIn(List<AppointmentStatus>)}:
     * o Hibernate não aplica o {@code @ColumnTransformer} do enum nativo do Postgres
     * (`appointment_status`) aos parâmetros de uma cláusula IN vinda de uma query
     * derivada, e isso gera "operator does not exist: appointment_status = character
     * varying" em runtime — 500 em toda chamada, pra qualquer cliente. Comparações
     * escalares (=) já funcionam normalmente com esse enum (ver ChatMessage.role).
     */
    @Query("""
            select case when count(a) > 0 then true else false end
            from Appointment a
            where a.dealership.id = :dealershipId
              and a.scheduledAt = :scheduledAt
              and (a.status = :first or a.status = :second)
            """)
    boolean existsActiveConflict(
            @Param("dealershipId") UUID dealershipId,
            @Param("scheduledAt") OffsetDateTime scheduledAt,
            @Param("first") AppointmentStatus first,
            @Param("second") AppointmentStatus second);
}
