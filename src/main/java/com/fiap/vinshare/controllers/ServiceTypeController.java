package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.dealership.ServiceTypeResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.service.ServiceTypeService;
import com.fiap.vinshare.specs.ServiceTypeControllerSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/service-types")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ServiceTypeController implements ServiceTypeControllerSpecs {

    private final ServiceTypeService serviceTypeService;

    @Override
    @GetMapping
    public ResponseEntity<ApiSingleResponse<List<ServiceTypeResponseDTO>>> list() {
        return ResponseEntity.ok(ApiSingleResponse.of(serviceTypeService.listAll()));
    }
}
