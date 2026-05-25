package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.Warranty;
import com.fiap.vinshare.domain.entities.WarrantyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, UUID> {

    Optional<Warranty> findByVehicleId(UUID vehicleId);

    List<Warranty> findByStatus(WarrantyStatus status);
}
