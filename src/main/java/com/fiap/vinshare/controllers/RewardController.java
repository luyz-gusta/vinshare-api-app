package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.loyalty.RewardRequestDTO;
import com.fiap.vinshare.domain.dto.loyalty.RewardResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.service.RewardService;
import com.fiap.vinshare.specs.RewardControllerSpecs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** Escrita do catálogo de prêmios. A listagem continua em LoyaltyController (GET /loyalty/rewards). */
@RestController
@RequestMapping("/loyalty/rewards")
@RequiredArgsConstructor
public class RewardController implements RewardControllerSpecs {

    private final RewardService rewardService;

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSingleResponse<RewardResponseDTO>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiSingleResponse.of(rewardService.findById(id)));
    }

    @Override
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSingleResponse<RewardResponseDTO>> create(@Valid @RequestBody RewardRequestDTO request) {
        RewardResponseDTO created = rewardService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(ApiSingleResponse.of(created, "Prêmio criado"));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSingleResponse<RewardResponseDTO>> update(@PathVariable UUID id,
                                                                       @Valid @RequestBody RewardRequestDTO request) {
        return ResponseEntity.ok(ApiSingleResponse.of(rewardService.update(id, request), "Prêmio atualizado"));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        rewardService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
