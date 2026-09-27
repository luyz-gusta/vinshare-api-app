package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.RefreshToken;
import com.fiap.vinshare.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.user = :user AND r.revokedAt IS NULL")
    void revokeAllForUser(@Param("user") User user, @Param("now") OffsetDateTime now);

    /**
     * Encerra todas as sessões do usuário apagando os refresh tokens. Apagar (e não
     * revogar) evita que a detecção de reuso trate como roubo um token encerrado
     * por troca de senha ou anonimização.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user = :user")
    int deleteAllByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff OR (r.revokedAt IS NOT NULL AND r.revokedAt < :cutoff)")
    int deleteExpiredOrRevokedBefore(@Param("cutoff") OffsetDateTime cutoff);
}
