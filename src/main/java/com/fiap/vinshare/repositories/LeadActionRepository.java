package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.LeadAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadActionRepository extends JpaRepository<LeadAction, UUID> {

    List<LeadAction> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
