package com.fiap.vinshare.domain.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequestDTO(
        @NotBlank @Size(max = 2000) String message
) {}
