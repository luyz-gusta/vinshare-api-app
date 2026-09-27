package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.loyalty.RewardRequestDTO;
import com.fiap.vinshare.domain.dto.loyalty.RewardResponseDTO;
import com.fiap.vinshare.domain.entities.Reward;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.infra.security.InputSanitizer;
import com.fiap.vinshare.repositories.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Catálogo de prêmios de fidelidade. Escrita restrita ao ADMIN (ver RewardController). */
@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;
    private final InputSanitizer sanitizer;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public RewardResponseDTO findById(UUID id) {
        return toDTO(require(id));
    }

    @Transactional
    public RewardResponseDTO create(RewardRequestDTO req) {
        Reward reward = rewardRepository.save(Reward.builder()
                .name(sanitizer.sanitize(req.name()))
                .description(sanitizer.sanitize(req.description()))
                .pointsCost(req.pointsCost())
                .active(req.active() == null || req.active())
                .build());
        auditService.record(AuditService.CONFIG_CHANGED, "rewards", reward.getId(), Map.of("operation", "CREATE"));
        return toDTO(reward);
    }

    @Transactional
    public RewardResponseDTO update(UUID id, RewardRequestDTO req) {
        Reward reward = require(id);
        reward.setName(sanitizer.sanitize(req.name()));
        reward.setDescription(sanitizer.sanitize(req.description()));
        reward.setPointsCost(req.pointsCost());
        if (req.active() != null) reward.setActive(req.active());
        reward = rewardRepository.save(reward);
        auditService.record(AuditService.CONFIG_CHANGED, "rewards", reward.getId(), Map.of("operation", "UPDATE"));
        return toDTO(reward);
    }

    /** Desativação lógica: preserva o histórico de resgates que referenciam o prêmio. */
    @Transactional
    public void deactivate(UUID id) {
        Reward reward = require(id);
        reward.setActive(false);
        rewardRepository.save(reward);
        auditService.record(AuditService.CONFIG_CHANGED, "rewards", reward.getId(), Map.of("operation", "DEACTIVATE"));
    }

    private Reward require(UUID id) {
        return rewardRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Prêmio", id));
    }

    private RewardResponseDTO toDTO(Reward r) {
        return RewardResponseDTO.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .pointsCost(r.getPointsCost())
                .active(r.isActive())
                .build();
    }
}
