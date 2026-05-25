package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.MaintenanceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceAlertRepository extends JpaRepository<MaintenanceAlert, UUID> {

    List<MaintenanceAlert> findAllByVehicleIdAndDismissedAtIsNullOrderByDueDateAsc(UUID vehicleId);

    List<MaintenanceAlert> findAllByDismissedAtIsNullAndDueDateBefore(java.time.LocalDate limit);
}
