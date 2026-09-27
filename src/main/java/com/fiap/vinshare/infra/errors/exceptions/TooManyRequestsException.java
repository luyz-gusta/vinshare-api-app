package com.fiap.vinshare.infra.errors.exceptions;

import lombok.Getter;

/** Limite de tentativas atingido. Mapeada para 429 com header Retry-After. */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
