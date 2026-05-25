package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.service.ServiceRecordResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.infra.security.SecurityUtils;
import com.fiap.vinshare.service.ServiceRecordService;
import com.fiap.vinshare.specs.ServiceRecordControllerSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ServiceRecordController implements ServiceRecordControllerSpecs {

    private final ServiceRecordService serviceRecordService;

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ApiSingleResponse<ServiceRecordResponseDTO>> findOne(@PathVariable UUID id) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(serviceRecordService.findOwnedById(id, user)));
    }
}
