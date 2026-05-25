package com.fiap.vinshare.domain.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequestDTO(
        @NotBlank @Size(max = 180) String fullName,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos numéricos") String cpf,
        @Size(max = 30) String phone,
        LocalDate birthDate,
        boolean lgpdConsent
) {}
