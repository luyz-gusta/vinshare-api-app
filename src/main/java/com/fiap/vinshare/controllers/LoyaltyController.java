package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.loyalty.LoyaltyBalanceDTO;
import com.fiap.vinshare.domain.dto.loyalty.LoyaltyTransactionDTO;
import com.fiap.vinshare.domain.dto.loyalty.RedeemRequestDTO;
import com.fiap.vinshare.domain.dto.loyalty.RedeemResponseDTO;
import com.fiap.vinshare.domain.dto.loyalty.RewardResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.infra.security.SecurityUtils;
import com.fiap.vinshare.service.LoyaltyService;
import com.fiap.vinshare.specs.LoyaltyControllerSpecs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class LoyaltyController implements LoyaltyControllerSpecs {

    private final LoyaltyService loyaltyService;

    @Override
    @GetMapping("/me/loyalty/balance")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiSingleResponse<LoyaltyBalanceDTO>> balance() {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(loyaltyService.getBalance(user)));
    }

    @Override
    @GetMapping("/me/loyalty/transactions")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiSingleResponse<Page<LoyaltyTransactionDTO>>> transactions(Pageable pageable) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(loyaltyService.listTransactions(user, pageable)));
    }

    @Override
    @GetMapping("/loyalty/rewards")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSingleResponse<List<RewardResponseDTO>>> rewards() {
        return ResponseEntity.ok(ApiSingleResponse.of(loyaltyService.listRewards()));
    }

    @Override
    @PostMapping("/me/loyalty/redeem")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiSingleResponse<RedeemResponseDTO>> redeem(@Valid @RequestBody RedeemRequestDTO request) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(loyaltyService.redeem(user, request), "Resgate efetuado"));
    }
}
