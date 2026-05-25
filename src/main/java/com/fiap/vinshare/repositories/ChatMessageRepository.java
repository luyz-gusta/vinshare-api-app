package com.fiap.vinshare.repositories;

import com.fiap.vinshare.domain.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    /** Remove mensagens anteriores à data de corte (política de retenção, Cyber frente 4). */
    @Modifying
    @Query("delete from ChatMessage m where m.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
