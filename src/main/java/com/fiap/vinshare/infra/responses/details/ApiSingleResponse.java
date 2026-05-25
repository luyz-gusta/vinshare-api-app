package com.fiap.vinshare.infra.responses.details;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * Envelope padrão de resposta para retornos de um único recurso.
 * Mantém consistência entre todos os endpoints da API.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiSingleResponse<T>(
        boolean success,
        String message,
        T data,
        OffsetDateTime timestamp
) {
    public static <T> ApiSingleResponse<T> of(T data) {
        return ApiSingleResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static <T> ApiSingleResponse<T> of(T data, String message) {
        return ApiSingleResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
