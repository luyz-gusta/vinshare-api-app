package com.fiap.vinshare.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"cpfEncrypted", "cpfLookupHash"})
public class Customer {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(name = "cpf_encrypted", nullable = false, columnDefinition = "TEXT")
    private String cpfEncrypted;

    @Column(name = "cpf_lookup_hash", nullable = false, unique = true, length = 64)
    private String cpfLookupHash;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "lgpd_consent_at")
    private OffsetDateTime lgpdConsentAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * CPF mascarado para exibição em UI. Não desencripta o valor real; devolve
     * apenas a estrutura mascarada quando o CPF está armazenado.
     */
    public String getCpfMasked() {
        return cpfEncrypted == null ? null : "***.***.***-**";
    }
}
