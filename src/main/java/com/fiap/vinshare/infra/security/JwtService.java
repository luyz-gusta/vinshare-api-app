package com.fiap.vinshare.infra.security;

import java.util.UUID;
import com.fiap.vinshare.domain.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;
    private SecretKey signingKey;
    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    void init() {
        if (props.getSecret() == null || props.getSecret().length() < 64) {
            throw new IllegalStateException(
                    "JWT_SECRET é obrigatório e deve ter pelo menos 64 caracteres. Ajuste a variável de ambiente.");
        }
        this.signingKey = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        log.info("JwtService inicializado. Issuer: {}", props.getIssuer());
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getAccessTokenMinutes() * 60L);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(props.getIssuer())
                .audience().add(props.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .signWith(signingKey)
                .compact();
    }

    public long accessTokenExpiresInSeconds() {
        return props.getAccessTokenMinutes() * 60L;
    }

    public long refreshTokenDays() {
        return props.getRefreshTokenDays();
    }

    /**
     * Valida assinatura, emissor, audiência e expiração. O papel usado na
     * autorização vem do banco (JwtAuthenticationFilter), não do claim "role":
     * mudar ou desativar um usuário tem efeito imediato.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(props.getIssuer())
                .requireAudience(props.getAudience())
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateRefreshTokenValue() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular hash do refresh token", e);
        }
    }
}
