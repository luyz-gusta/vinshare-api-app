package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByUser(User user);

    Optional<Customer> findByUserId(UUID userId);

    boolean existsByCpfLookupHash(String cpfLookupHash);
}
