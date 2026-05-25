package com.fiap.vinshare.infra.errors.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("%s não encontrado para o identificador %s".formatted(resource, id));
    }
}
