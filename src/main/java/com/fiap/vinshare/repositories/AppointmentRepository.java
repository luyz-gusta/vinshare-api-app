package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.Appointment;
import com.fiap.vinshare.domain.entities.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Page<Appointment> findAllByCustomerIdOrderByScheduledAtDesc(UUID customerId, Pageable pageable);

    Page<Appointment> findAllByCustomerIdAndStatusInOrderByScheduledAtDesc(
            UUID customerId, List<AppointmentStatus> statuses, Pageable pageable);

    Optional<Appointment> findByIdAndCustomerId(UUID id, UUID customerId);

    List<Appointment> findAllByDealershipIdAndScheduledAtBetweenAndStatusIn(
            UUID dealershipId, OffsetDateTime from, OffsetDateTime to, List<AppointmentStatus> statuses);

    boolean existsByDealershipIdAndScheduledAtAndStatusIn(
            UUID dealershipId, OffsetDateTime scheduledAt, List<AppointmentStatus> statuses);
}
