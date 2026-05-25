package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.vehicle.MaintenanceAlertResponseDTO;
import com.fiap.vinshare.domain.dto.vehicle.UpdateOdometerRequestDTO;
import com.fiap.vinshare.domain.dto.vehicle.VehicleResponseDTO;
import com.fiap.vinshare.domain.dto.vehicle.WarrantyResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.infra.security.SecurityUtils;
import com.fiap.vinshare.service.VehicleService;
import com.fiap.vinshare.specs.VehicleControllerSpecs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class VehicleController implements VehicleControllerSpecs {

    private final VehicleService vehicleService;

    @Override
    @GetMapping("/{vehicleId}/warranty")
    public ResponseEntity<ApiSingleResponse<WarrantyResponseDTO>> warranty(@PathVariable UUID vehicleId) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(vehicleService.getWarranty(vehicleId, user)));
    }

    @Override
    @GetMapping("/{vehicleId}/maintenance-alerts")
    public ResponseEntity<ApiSingleResponse<List<MaintenanceAlertResponseDTO>>> alerts(@PathVariable UUID vehicleId) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(vehicleService.getAlerts(vehicleId, user)));
    }

    @Override
    @PatchMapping("/{vehicleId}/odometer")
    public ResponseEntity<ApiSingleResponse<VehicleResponseDTO>> updateOdometer(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateOdometerRequestDTO request) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(vehicleService.updateOdometer(vehicleId, request, user)));
    }
}
