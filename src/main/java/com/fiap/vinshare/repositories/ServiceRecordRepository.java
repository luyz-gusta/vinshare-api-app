package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.ServiceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, UUID> {

    Page<ServiceRecord> findAllByVehicle_CustomerIdOrderByPerformedAtDesc(UUID customerId, Pageable pageable);

    Page<ServiceRecord> findAllByVehicle_CustomerIdAndVehicle_IdOrderByPerformedAtDesc(
            UUID customerId, UUID vehicleId, Pageable pageable);

    List<ServiceRecord> findAllByVehicleIdOrderByPerformedAtDesc(UUID vehicleId);

    Optional<ServiceRecord> findByAppointmentId(UUID appointmentId);

    List<ServiceRecord> findAllByPerformedAtBetween(OffsetDateTime start, OffsetDateTime end);
}
