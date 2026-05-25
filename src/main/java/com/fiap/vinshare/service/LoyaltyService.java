package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.loyalty.LoyaltyBalanceDTO;
import com.fiap.vinshare.domain.dto.loyalty.LoyaltyTransactionDTO;
import com.fiap.vinshare.domain.dto.loyalty.RedeemRequestDTO;
import com.fiap.vinshare.domain.dto.loyalty.RedeemResponseDTO;
import com.fiap.vinshare.domain.dto.loyalty.RewardResponseDTO;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.LoyaltyAccount;
import com.fiap.vinshare.domain.entities.LoyaltyTransaction;
import com.fiap.vinshare.domain.entities.LoyaltyTransactionType;
import com.fiap.vinshare.domain.entities.Reward;
import com.fiap.vinshare.domain.entities.ServiceRecord;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.infra.errors.exceptions.BusinessRuleException;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.LoyaltyAccountRepository;
import com.fiap.vinshare.repositories.LoyaltyTransactionRepository;
import com.fiap.vinshare.repositories.RewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String VOUCHER_PREFIX = "FORD-";

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository txRepository;
    private final RewardRepository rewardRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public LoyaltyBalanceDTO getBalance(User user) {
        LoyaltyAccount account = ensureAccount(requireCustomer(user));
        return LoyaltyBalanceDTO.builder()
                .balance(account.getBalance() == null ? 0 : account.getBalance())
                .expiringIn30Days(0)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyTransactionDTO> listTransactions(User user, Pageable pageable) {
        LoyaltyAccount account = ensureAccount(requireCustomer(user));
        return txRepository.findAllByAccountIdOrderByCreatedAtDesc(account.getId(), pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<RewardResponseDTO> listRewards() {
        return rewardRepository.findAllByActiveTrueOrderByPointsCostAsc().stream()
                .map(r -> RewardResponseDTO.builder()
                        .id(r.getId())
                        .name(r.getName())
                        .description(r.getDescription())
                        .pointsCost(r.getPointsCost())
                        .build())
                .toList();
    }

    @Transactional
    public RedeemResponseDTO redeem(User user, RedeemRequestDTO req) {
        Customer customer = requireCustomer(user);
        Reward reward = rewardRepository.findById(req.rewardId())
                .orElseThrow(() -> ResourceNotFoundException.of("Prêmio", req.rewardId()));
        if (!reward.isActive()) {
            throw new BusinessRuleException("Prêmio indisponível");
        }
        LoyaltyAccount account = ensureAccount(customer);
        int balance = account.getBalance() == null ? 0 : account.getBalance();
        if (balance < reward.getPointsCost()) {
            throw new BusinessRuleException("Saldo insuficiente para resgate");
        }

        account.subtract(reward.getPointsCost());
        accountRepository.save(account);

        String voucher = generateVoucher();
        LoyaltyTransaction tx = LoyaltyTransaction.builder()
                .account(account)
                .type(LoyaltyTransactionType.REDEEM)
                .points(reward.getPointsCost())
                .sourceReward(reward)
                .voucherCode(voucher)
                .build();
        tx = txRepository.save(tx);

        log.info("Resgate efetuado: cliente={}, prêmio={}, voucher={}",
                customer.getId(), reward.getId(), voucher);

        return RedeemResponseDTO.builder()
                .transactionId(tx.getId())
                .rewardId(reward.getId())
                .rewardName(reward.getName())
                .voucherCode(voucher)
                .expiresAt(OffsetDateTime.now().plusMonths(3))
                .pointsSpent(reward.getPointsCost())
                .newBalance(account.getBalance())
                .build();
    }

    /**
     * Acumula pontos quando um serviço é concluído.
     * Chamado por {@link AppointmentService#complete}.
     */
    @Transactional
    public void earnFromService(Customer customer, ServiceRecord service, int points) {
        if (points <= 0) return;
        LoyaltyAccount account = ensureAccount(customer);
        account.add(points);
        accountRepository.save(account);

        txRepository.save(LoyaltyTransaction.builder()
                .account(account)
                .type(LoyaltyTransactionType.EARN)
                .points(points)
                .sourceService(service)
                .build());
    }

    private LoyaltyAccount ensureAccount(Customer customer) {
        return accountRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> accountRepository.save(LoyaltyAccount.builder()
                        .customer(customer)
                        .balance(0)
                        .build()));
    }

    private Customer requireCustomer(User user) {
        return customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
    }

    private String generateVoucher() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(VOUCHER_PREFIX);
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    private LoyaltyTransactionDTO toDTO(LoyaltyTransaction tx) {
        return LoyaltyTransactionDTO.builder()
                .id(tx.getId())
                .type(tx.getType())
                .points(tx.getPoints())
                .sourceServiceId(tx.getSourceService() == null ? null : tx.getSourceService().getId())
                .sourceRewardId(tx.getSourceReward() == null ? null : tx.getSourceReward().getId())
                .rewardName(tx.getSourceReward() == null ? null : tx.getSourceReward().getName())
                .voucherCode(tx.getVoucherCode())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
