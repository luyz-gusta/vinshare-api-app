package com.fiap.vinshare;

import com.fiap.vinshare.domain.dto.auth.LoginRequestDTO;
import com.fiap.vinshare.infra.errors.exceptions.InvalidCredentialsException;
import com.fiap.vinshare.service.AuthService;
import com.fiap.vinshare.support.ConcurrencyTest;
import com.fiap.vinshare.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Força bruta distribuída: uma rajada de logins inválidos acima do tamanho do pool
 * precisa responder 401 a todos, sem travar o pool com a gravação da auditoria.
 */
class LoginConcurrencyTest extends ConcurrencyTest {

    private static final String UNAUTHORIZED = "401";

    @Autowired
    private AuthService authService;

    @Test
    void rajadaDeLoginsInvalidosAcimaDoPoolResponde401ATodos() throws Exception {
        warmUpPool();

        int threads = poolSize + 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String email = TestData.uniqueEmail("rajada");
            results.add(pool.submit(() -> {
                start.await();
                try {
                    authService.login(new LoginRequestDTO(email, "senha-errada-123"));
                    return "logou";
                } catch (InvalidCredentialsException e) {
                    return UNAUTHORIZED;
                } catch (RuntimeException e) {
                    return e.getClass().getSimpleName() + ": " + e.getMessage();
                }
            }));
        }
        start.countDown();

        List<String> outcomes = new ArrayList<>();
        for (Future<String> f : results) {
            outcomes.add(f.get(90, TimeUnit.SECONDS));
        }
        pool.shutdownNow();

        assertThat(outcomes).containsOnly(UNAUTHORIZED);
    }
}
