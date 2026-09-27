package com.fiap.vinshare;

import com.fiap.vinshare.domain.dto.auth.AuthResponseDTO;
import com.fiap.vinshare.domain.dto.auth.LoginRequestDTO;
import com.fiap.vinshare.domain.dto.auth.RefreshRequestDTO;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.infra.errors.exceptions.InvalidCredentialsException;
import com.fiap.vinshare.service.AuthService;
import com.fiap.vinshare.support.ConcurrencyTest;
import com.fiap.vinshare.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Refresh token é de uso único mesmo sob concorrência: várias renovações simultâneas
 * com o mesmo token emitem uma única sessão nova, e as demais não derrubam essa sessão.
 */
class RefreshConcurrencyTest extends ConcurrencyTest {

    @Autowired
    private AuthService authService;

    @Test
    void refreshSimultaneoDoMesmoTokenRenovaUmaUnicaVez() throws Exception {
        Customer customer = fixtures.customer();
        String r1 = authService.login(new LoginRequestDTO(customer.getUser().getEmail(), TestFixtures.PASSWORD))
                .refreshToken();
        warmUpPool();

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Optional<AuthResponseDTO>>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    return Optional.of(authService.refresh(new RefreshRequestDTO(r1)));
                } catch (InvalidCredentialsException e) {
                    return Optional.<AuthResponseDTO>empty();
                }
            }));
        }
        start.countDown();

        List<AuthResponseDTO> renewed = new ArrayList<>();
        for (Future<Optional<AuthResponseDTO>> f : results) {
            f.get(60, TimeUnit.SECONDS).ifPresent(renewed::add);
        }
        pool.shutdownNow();

        assertThat(renewed).hasSize(1);
        // A sessão renovada continua válida: as outras tentativas não dispararam a revogação da família.
        assertThat(authService.refresh(new RefreshRequestDTO(renewed.get(0).refreshToken())).accessToken()).isNotBlank();
    }
}
