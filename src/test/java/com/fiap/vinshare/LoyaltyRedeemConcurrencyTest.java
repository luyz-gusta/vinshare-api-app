package com.fiap.vinshare;

import com.fiap.vinshare.domain.dto.loyalty.RedeemRequestDTO;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.Reward;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.infra.errors.exceptions.BusinessRuleException;
import com.fiap.vinshare.repositories.LoyaltyAccountRepository;
import com.fiap.vinshare.service.LoyaltyService;
import com.fiap.vinshare.support.IntegrationTest;
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

/** Duplo toque ou script: resgates simultâneos não podem gastar o mesmo saldo duas vezes. */
class LoyaltyRedeemConcurrencyTest extends IntegrationTest {

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private LoyaltyAccountRepository accountRepository;

    @Test
    void resgatesSimultaneosSoPermitemUmSucesso() throws Exception {
        Customer customer = fixtures.customer();
        Reward reward = fixtures.reward(100);
        fixtures.setBalance(customer, 100);
        User user = customer.getUser();

        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    loyaltyService.redeem(user, new RedeemRequestDTO(reward.getId()));
                    return true;
                } catch (BusinessRuleException e) {
                    return false;
                }
            }));
        }
        start.countDown();

        int successes = 0;
        for (Future<Boolean> f : results) {
            if (f.get(30, TimeUnit.SECONDS)) successes++;
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(accountRepository.findByCustomerId(customer.getId()).orElseThrow().getBalance()).isZero();
    }
}
