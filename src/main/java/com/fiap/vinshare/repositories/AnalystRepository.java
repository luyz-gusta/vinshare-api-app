package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.Analyst;
import com.fiap.vinshare.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalystRepository extends JpaRepository<Analyst, UUID> {

    Optional<Analyst> findByUser(User user);

    Optional<Analyst> findByUserId(UUID userId);
}
