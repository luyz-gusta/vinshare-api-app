package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.LoyaltyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, UUID> {

    Page<LoyaltyTransaction> findAllByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
}
