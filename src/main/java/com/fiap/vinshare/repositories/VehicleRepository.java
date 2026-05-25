package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findAllByCustomerId(UUID customerId);

    Optional<Vehicle> findByIdAndCustomerId(UUID id, UUID customerId);
}
