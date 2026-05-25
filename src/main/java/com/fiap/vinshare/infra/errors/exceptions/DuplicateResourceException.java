package com.fiap.vinshare.infra.errors.exceptions;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public static DuplicateResourceException of(String resource, String field, Object value) {
        return new DuplicateResourceException("%s com %s '%s' já cadastrado.".formatted(resource, field, value));
    }
}
