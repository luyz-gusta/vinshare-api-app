package com.fiap.vinshare.repositories;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import com.fiap.vinshare.domain.entities.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, UUID> {

    Optional<LoyaltyAccount> findByCustomerId(UUID customerId);

    /** SELECT ... FOR UPDATE: serializa operações de saldo da mesma conta. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM LoyaltyAccount a WHERE a.customer.id = :customerId")
    Optional<LoyaltyAccount> findByCustomerIdForUpdate(@Param("customerId") UUID customerId);
}
