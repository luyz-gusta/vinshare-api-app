package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /** Remove eventos de auditoria anteriores à data de corte (política de retenção, Cyber frente 4). */
    @Modifying
    @Query("delete from AuditLog a where a.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
