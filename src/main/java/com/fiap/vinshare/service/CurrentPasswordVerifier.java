package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.infra.errors.exceptions.BusinessValidationException;
import com.fiap.vinshare.infra.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Reautenticação para ações sensíveis (troca de senha, exclusão da conta): confere a
 * senha atual. Erro responde 422, e não 401: o usuário está autenticado, e 401 faria o
 * app encerrar a sessão. Erros contam no mesmo limite do login (LoginAttemptService),
 * para barrar força bruta com um access token roubado.
 */
@Component
@RequiredArgsConstructor
public class CurrentPasswordVerifier {

    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public void verify(User user, String currentPassword) {
        String email = user.getEmail();
        loginAttemptService.checkAllowed(email);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            loginAttemptService.onFailure(email);
            throw new BusinessValidationException("Senha atual incorreta");
        }
        loginAttemptService.onSuccess(email);
    }
}
