package com.fiap.vinshare.domain.dto.privacy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A exclusão é irreversível: exige a senha atual, e não só o access token. */
public record DeleteAccountRequestDTO(
        @NotBlank @Size(max = 100) String currentPassword
) {}
