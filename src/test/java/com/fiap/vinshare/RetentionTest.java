package com.fiap.vinshare;

import com.fiap.vinshare.domain.entities.RefreshToken;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.repositories.RefreshTokenRepository;
import com.fiap.vinshare.service.RetentionService;
import com.fiap.vinshare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "app.retention.enabled=true")
class RetentionTest extends IntegrationTest {

    @Autowired
    private RetentionService retentionService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshToken token(User user, OffsetDateTime expiresAt, OffsetDateTime revokedAt) {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        return refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(HexFormat.of().formatHex(raw))
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .build());
    }

    @Test
    void removeRefreshTokensVencidosOuRevogadosHaMaisDe30Dias() {
        User user = fixtures.customer().getUser();
        OffsetDateTime old = OffsetDateTime.now().minusDays(40);
        RefreshToken antigo = token(user, old, old);
        RefreshToken vigente = token(user, OffsetDateTime.now().plusDays(5), null);

        retentionService.purgeExpiredRecords();

        assertThat(refreshTokenRepository.findById(antigo.getId())).isEmpty();
        assertThat(refreshTokenRepository.findById(vigente.getId())).isPresent();
    }
}
