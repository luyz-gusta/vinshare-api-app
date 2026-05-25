package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.dealership.DealershipAvailabilityResponseDTO;
import com.fiap.vinshare.domain.dto.dealership.DealershipResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.service.DealershipService;
import com.fiap.vinshare.specs.DealershipControllerSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dealerships")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DealershipController implements DealershipControllerSpecs {

    private final DealershipService dealershipService;

    @Override
    @GetMapping
    public ResponseEntity<ApiSingleResponse<List<DealershipResponseDTO>>> nearby(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String service) {
        return ResponseEntity.ok(ApiSingleResponse.of(
                dealershipService.findNearby(lat, lng, radiusKm, service)));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ApiSingleResponse<DealershipResponseDTO>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiSingleResponse.of(dealershipService.findById(id)));
    }

    @Override
    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiSingleResponse<DealershipAvailabilityResponseDTO>> availability(
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String serviceType) {
        return ResponseEntity.ok(ApiSingleResponse.of(
                dealershipService.getAvailability(id, date, serviceType)));
    }
}
