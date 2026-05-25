package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.DeviceToken;
import com.fiap.vinshare.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findAllByUserAndRevokedAtIsNull(User user);
}
