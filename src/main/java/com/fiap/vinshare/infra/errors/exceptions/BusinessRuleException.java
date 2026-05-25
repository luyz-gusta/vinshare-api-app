package com.fiap.vinshare.infra.errors.exceptions;

/**
 * Lançada quando uma regra de negócio é violada (ex.: tentar completar um
 * agendamento já cancelado, resgatar prêmio sem saldo, etc.).
 *
 * Mapeada para HTTP 409 Conflict pelo GlobalExceptionHandler.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
