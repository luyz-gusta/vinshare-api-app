package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.device.DeviceResponseDTO;
import com.fiap.vinshare.domain.dto.device.RegisterDeviceRequestDTO;
import com.fiap.vinshare.domain.entities.DeviceToken;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.repositories.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Registra (ou reativa) um token de push para o usuário. Idempotente:
     * se o token já existe, reativa e reassocia ao usuário atual.
     */
    @Transactional
    public DeviceResponseDTO register(User user, RegisterDeviceRequestDTO request) {
        DeviceToken device = deviceTokenRepository.findByToken(request.token())
                .map(existing -> {
                    existing.setUser(user);
                    existing.setPlatform(request.platform());
                    existing.setRevokedAt(null);
                    return existing;
                })
                .orElseGet(() -> DeviceToken.builder()
                        .user(user)
                        .token(request.token())
                        .platform(request.platform())
                        .build());

        device = deviceTokenRepository.save(device);
        return toDTO(device);
    }

    @Transactional
    public void revoke(User user, String token) {
        deviceTokenRepository.findByToken(token)
                .filter(d -> d.getUser().getId().equals(user.getId()))
                .ifPresent(DeviceToken::revoke);
    }

    @Transactional(readOnly = true)
    public List<DeviceResponseDTO> listActive(User user) {
        return deviceTokenRepository.findAllByUserAndRevokedAtIsNull(user).stream()
                .map(this::toDTO)
                .toList();
    }

    private DeviceResponseDTO toDTO(DeviceToken device) {
        return DeviceResponseDTO.builder()
                .id(device.getId())
                .token(device.getToken())
                .platform(device.getPlatform())
                .createdAt(device.getCreatedAt())
                .build();
    }
}
