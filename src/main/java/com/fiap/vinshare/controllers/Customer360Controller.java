package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.customer.Customer360DTO;
import com.fiap.vinshare.domain.dto.customer.TimelineEventDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.service.Customer360Service;
import com.fiap.vinshare.specs.Customer360ControllerSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
public class Customer360Controller implements Customer360ControllerSpecs {

    private final Customer360Service customer360Service;

    @Override
    @GetMapping("/{customerId}/360")
    public ResponseEntity<ApiSingleResponse<Customer360DTO>> get360(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ApiSingleResponse.of(customer360Service.get360(customerId)));
    }

    @Override
    @GetMapping("/{customerId}/timeline")
    public ResponseEntity<ApiSingleResponse<List<TimelineEventDTO>>> timeline(
            @PathVariable UUID customerId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        return ResponseEntity.ok(ApiSingleResponse.of(customer360Service.timeline(customerId, from, to)));
    }
}
