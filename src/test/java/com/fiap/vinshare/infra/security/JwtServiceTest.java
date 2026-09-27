package com.fiap.vinshare.infra.security;

import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.domain.entities.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "chave-de-teste-com-no-minimo-sessenta-e-quatro-caracteres-aleatorios-1234567890";

    private JwtService service;
    private User user;

    private static JwtService build(String secret) {
        JwtProperties props = new JwtProperties();
        ReflectionTestUtils.setField(props, "secret", secret);
        ReflectionTestUtils.setField(props, "accessTokenMinutes", 15);
        ReflectionTestUtils.setField(props, "refreshTokenDays", 7);
        ReflectionTestUtils.setField(props, "issuer", "ford-vinshare-api");
        ReflectionTestUtils.setField(props, "audience", "vinshare-clients");
        JwtService jwt = new JwtService(props);
        ReflectionTestUtils.invokeMethod(jwt, "init");
        return jwt;
    }

    @BeforeEach
    void setUp() {
        service = build(SECRET);
        user = User.builder().id(UUID.randomUUID()).email("a@b.com").role(UserRole.CLIENT).build();
    }

    @Test
    void tokenContemJtiAudienceEIssuer() {
        Claims claims = service.parse(service.generateAccessToken(user));
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.getIssuer()).isEqualTo("ford-vinshare-api");
        assertThat(claims.getAudience()).containsExactly("vinshare-clients");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.get("role", String.class)).isEqualTo("CLIENT");
    }

    @Test
    void tokensConsecutivosTemJtiDiferentes() {
        String a = service.parse(service.generateAccessToken(user)).getId();
        String b = service.parse(service.generateAccessToken(user)).getId();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void rejeitaTokenDeOutraAudiencia() {
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("ford-vinshare-api")
                .audience().add("outro-sistema").and()
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(5))))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejeitaTokenSemAudience() {
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("ford-vinshare-api")
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(5))))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejeitaTokenExpirado() {
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("ford-vinshare-api")
                .audience().add("vinshare-clients").and()
                .expiration(Date.from(Instant.now().minus(Duration.ofMinutes(5))))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejeitaTokenAssinadoComOutraChave() {
        String outro = build(SECRET.replace('a', 'b')).generateAccessToken(user);
        assertThatThrownBy(() -> service.parse(outro)).isInstanceOf(JwtException.class);
    }

    @Test
    void initFalhaComSegredoCurto() {
        assertThatThrownBy(() -> build("curto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
