package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.PartUsed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartUsedRepository extends JpaRepository<PartUsed, UUID> {

    List<PartUsed> findAllByServiceId(UUID serviceId);
}
