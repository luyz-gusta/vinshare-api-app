package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.auth.AuthResponseDTO;
import com.fiap.vinshare.domain.dto.auth.LoginRequestDTO;
import com.fiap.vinshare.domain.dto.auth.RefreshRequestDTO;
import com.fiap.vinshare.domain.dto.auth.RegisterRequestDTO;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.RefreshToken;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.domain.entities.UserRole;
import com.fiap.vinshare.infra.errors.exceptions.DuplicateResourceException;
import com.fiap.vinshare.infra.errors.exceptions.InvalidCredentialsException;
import com.fiap.vinshare.infra.security.CryptoService;
import com.fiap.vinshare.infra.security.InputSanitizer;
import com.fiap.vinshare.infra.security.JwtService;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.RefreshTokenRepository;
import com.fiap.vinshare.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CryptoService cpfCryptoService;
    private final InputSanitizer sanitizer;
    private final AuditService auditService;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }
        String cpfLookup = cpfCryptoService.hash(request.cpf());
        if (customerRepository.existsByCpfLookupHash(cpfLookup)) {
            throw new DuplicateResourceException("CPF já cadastrado");
        }

        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.CLIENT)
                .active(true)
                .build();
        user = userRepository.save(user);

        Customer customer = Customer.builder()
                .user(user)
                .fullName(sanitizer.sanitize(request.fullName()))
                .cpfEncrypted(cpfCryptoService.encrypt(request.cpf()))
                .cpfLookupHash(cpfLookup)
                .phone(request.phone())
                .birthDate(request.birthDate())
                .lgpdConsentAt(request.lgpdConsent() ? OffsetDateTime.now() : null)
                .build();
        customerRepository.save(customer);

        log.info("Novo cliente registrado: userId={}", user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        String email = request.email().toLowerCase().trim();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !user.isActive()
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditService.loginFailure(email);
            throw new InvalidCredentialsException("E-mail ou senha inválidos");
        }

        AuthResponseDTO response = buildAuthResponse(user);
        auditService.loginSuccess(user.getId(), user.getEmail());
        return response;
    }

    @Transactional
    public AuthResponseDTO refresh(RefreshRequestDTO request) {
        String hash = jwtService.hashRefreshToken(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token inválido"));

        if (!stored.isActive()) {
            throw new InvalidCredentialsException("Refresh token expirado ou revogado");
        }

        stored.revoke();
        refreshTokenRepository.save(stored);

        return buildAuthResponse(stored.getUser());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String hash = jwtService.hashRefreshToken(refreshToken);
        Optional<RefreshToken> stored = refreshTokenRepository.findByTokenHash(hash);
        stored.ifPresent(rt -> {
            rt.revoke();
            refreshTokenRepository.save(rt);
        });
    }

    private AuthResponseDTO buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshPlain = jwtService.generateRefreshTokenValue();
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtService.hashRefreshToken(refreshPlain))
                .expiresAt(OffsetDateTime.now().plusDays(jwtService.refreshTokenDays()))
                .build();
        refreshTokenRepository.save(stored);

        return AuthResponseDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshPlain)
                .tokenType("Bearer")
                .expiresIn(jwtService.accessTokenExpiresInSeconds())
                .build();
    }
}
