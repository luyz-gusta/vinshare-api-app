package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.lead.LeadActionRequestDTO;
import com.fiap.vinshare.domain.dto.lead.LeadActionResponseDTO;
import com.fiap.vinshare.domain.dto.lead.LeadResponseDTO;
import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import com.fiap.vinshare.domain.entities.LeadHealthStatus;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.infra.security.SecurityUtils;
import com.fiap.vinshare.service.LeadService;
import com.fiap.vinshare.specs.LeadControllerSpecs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
public class LeadController implements LeadControllerSpecs {

    private final LeadService leadService;

    @Override
    @GetMapping
    public ResponseEntity<ApiSingleResponse<Page<LeadResponseDTO>>> list(
            @RequestParam(required = false) CustomerSegmentType segment,
            @RequestParam(required = false) LeadHealthStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiSingleResponse.of(leadService.listLeads(segment, status, pageable)));
    }

    @Override
    @GetMapping("/{customerId}")
    public ResponseEntity<ApiSingleResponse<LeadResponseDTO>> findOne(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ApiSingleResponse.of(leadService.findById(customerId)));
    }

    @Override
    @PostMapping("/{customerId}/actions")
    public ResponseEntity<ApiSingleResponse<LeadActionResponseDTO>> triggerAction(
            @PathVariable UUID customerId,
            @Valid @RequestBody LeadActionRequestDTO request) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiSingleResponse.of(leadService.triggerAction(customerId, request, user), "Ação enfileirada"));
    }
}
