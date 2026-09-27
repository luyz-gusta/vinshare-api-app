package com.fiap.vinshare.service;

import com.fiap.vinshare.repositories.RefreshTokenRepository;
import com.fiap.vinshare.infra.security.LoginAttemptService;
import com.fiap.vinshare.infra.errors.exceptions.BusinessValidationException;
import com.fiap.vinshare.domain.dto.auth.ChangePasswordRequestDTO;
import com.fiap.vinshare.domain.dto.auth.MeResponseDTO;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public MeResponseDTO buildMe(User user) {
        String fullName = user.getDisplayName();
        String phone = null;

        var customer = customerRepository.findByUser(user).orElse(null);
        if (customer != null) {
            if (fullName == null) fullName = customer.getFullName();
            phone = customer.getPhone();
        }

        return MeResponseDTO.builder()
                .userId(user.getId())
                .role(user.getRole())
                .email(user.getEmail())
                .fullName(fullName)
                .phone(phone)
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Troca a senha exigindo a atual. Erro na senha atual responde 422, e não 401:
     * o usuário está autenticado, e 401 faria o app encerrar a sessão. Erros contam
     * no mesmo limite do login (LoginAttemptService), para barrar força bruta com
     * um access token roubado. Ao trocar, encerra todas as sessões (refresh tokens).
     */
    @Transactional
    public void changePassword(User user, ChangePasswordRequestDTO request) {
        String email = user.getEmail();
        loginAttemptService.checkAllowed(email);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            loginAttemptService.onFailure(email);
            throw new BusinessValidationException("Senha atual incorreta");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessValidationException("A nova senha deve ser diferente da atual");
        }

        loginAttemptService.onSuccess(email);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.deleteAllByUser(user);
        auditService.record(AuditService.PASSWORD_CHANGED, "users", user.getId(), java.util.Map.of());
    }
}
