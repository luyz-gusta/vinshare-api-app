package com.fiap.vinshare;

import com.fiap.vinshare.domain.dto.loyalty.RedeemRequestDTO;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.Reward;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.infra.errors.exceptions.BusinessRuleException;
import com.fiap.vinshare.repositories.LoyaltyAccountRepository;
import com.fiap.vinshare.service.LoyaltyService;
import com.fiap.vinshare.support.ConcurrencyTest;
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
 * Duplo toque ou script: resgates simultâneos não podem gastar o mesmo saldo duas vezes.
 * Usa mais threads que conexões no pool: quem segura o lock do saldo não pode
 * depender de uma segunda conexão (a auditoria) para terminar.
 */
class LoyaltyRedeemConcurrencyTest extends ConcurrencyTest {

    private static final String OK = "resgatou";
    private static final String NO_BALANCE = "saldo insuficiente";

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private LoyaltyAccountRepository accountRepository;

    @Test
    void resgatesSimultaneosAcimaDoPoolSoPermitemUmSucesso() throws Exception {
        Customer customer = fixtures.customer();
        Reward reward = fixtures.reward(100);
        fixtures.setBalance(customer, 100);
        User user = customer.getUser();
        warmUpPool();

        int threads = poolSize + 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    loyaltyService.redeem(user, new RedeemRequestDTO(reward.getId()));
                    return OK;
                } catch (BusinessRuleException e) {
                    return NO_BALANCE;
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

        assertThat(outcomes).containsOnly(OK, NO_BALANCE);
        assertThat(outcomes).filteredOn(OK::equals).hasSize(1);
        assertThat(accountRepository.findByCustomerId(customer.getId()).orElseThrow().getBalance()).isZero();
    }
}
