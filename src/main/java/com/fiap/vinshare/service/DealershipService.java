package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.dealership.AvailabilitySlotDTO;
import com.fiap.vinshare.domain.dto.dealership.DealershipAvailabilityResponseDTO;
import com.fiap.vinshare.domain.dto.dealership.DealershipResponseDTO;
import com.fiap.vinshare.domain.entities.Dealership;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.repositories.DealershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DealershipService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final DealershipRepository dealershipRepository;

    @Transactional(readOnly = true)
    public List<DealershipResponseDTO> findNearby(BigDecimal lat, BigDecimal lng,
                                                  Double radiusKm, String serviceTypeId) {
        double effectiveRadius = radiusKm == null ? 25.0 : radiusKm;
        return dealershipRepository.findNearby(lat, lng, effectiveRadius, serviceTypeId).stream()
                .map(d -> toDTO(d, distanceKm(lat, lng, d.getLat(), d.getLng())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DealershipResponseDTO findById(UUID id) {
        Dealership d = dealershipRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Concessionária", id));
        return toDTO(d, null);
    }

    @Transactional(readOnly = true)
    public DealershipAvailabilityResponseDTO getAvailability(UUID dealershipId, LocalDate date,
                                                             String serviceTypeId) {
        // Garante que existe; a lógica real de slots aparece na Fase 3 (agendamentos).
        // Para a Fase 2 devolvemos slots fixos de 08:00 a 17:00 todos disponíveis.
        if (!dealershipRepository.existsById(dealershipId)) {
            throw ResourceNotFoundException.of("Concessionária", dealershipId);
        }
        List<AvailabilitySlotDTO> slots = java.util.stream.IntStream.rangeClosed(8, 17)
                .mapToObj(h -> AvailabilitySlotDTO.builder()
                        .time(LocalTime.of(h, 0))
                        .available(true)
                        .build())
                .toList();
        return DealershipAvailabilityResponseDTO.builder()
                .date(date == null ? LocalDate.now() : date)
                .slots(slots)
                .build();
    }

    private DealershipResponseDTO toDTO(Dealership d, Double distance) {
        return DealershipResponseDTO.builder()
                .id(d.getId())
                .name(d.getName())
                .address(d.getAddress())
                .city(d.getCity())
                .state(d.getState())
                .phone(d.getPhone())
                .lat(d.getLat())
                .lng(d.getLng())
                .distanceKm(distance)
                .openingHours(d.getOpeningHours())
                .build();
    }

    private Double distanceKm(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return null;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0;
    }
}
