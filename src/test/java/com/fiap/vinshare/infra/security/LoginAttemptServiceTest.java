package com.fiap.vinshare.infra.security;

import com.fiap.vinshare.infra.errors.exceptions.TooManyRequestsException;
import com.fiap.vinshare.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LoginAttemptServiceTest {

    private AuditService audit;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        audit = mock(AuditService.class);
        service = new LoginAttemptService(3, 10, audit);
    }

    @Test
    void bloqueiaAposAtingirOLimiteEAuditaUmaVez() {
        for (int i = 0; i < 3; i++) service.onFailure("a@b.com");
        assertThatThrownBy(() -> service.checkAllowed("a@b.com")).isInstanceOf(TooManyRequestsException.class);
        service.onFailure("a@b.com");
        verify(audit, times(1)).suspiciousLogin(eq("a@b.com"), eq(3));
    }

    @Test
    void sucessoZeraOContador() {
        service.onFailure("a@b.com");
        service.onFailure("a@b.com");
        service.onSuccess("a@b.com");
        service.onFailure("a@b.com");
        assertThatCode(() -> service.checkAllowed("a@b.com")).doesNotThrowAnyException();
    }

    @Test
    void contadorEPorEmail() {
        for (int i = 0; i < 3; i++) service.onFailure("a@b.com");
        assertThatCode(() -> service.checkAllowed("c@d.com")).doesNotThrowAnyException();
    }
}
