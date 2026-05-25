package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, UUID> {

    Optional<LoyaltyAccount> findByCustomerId(UUID customerId);
}
