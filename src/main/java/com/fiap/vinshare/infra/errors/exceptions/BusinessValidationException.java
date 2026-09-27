package com.fiap.vinshare.infra.errors.exceptions;

/**
 * Requisição bem formada, mas semanticamente inválida para o domínio
 * (ex.: agendar no passado, odômetro regredindo). Mapeada para 422.
 */
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}
