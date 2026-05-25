package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, String> {
}
