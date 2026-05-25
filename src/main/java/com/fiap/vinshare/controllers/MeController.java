package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.appointment.AppointmentResponseDTO;
import com.fiap.vinshare.domain.dto.auth.MeResponseDTO;
import com.fiap.vinshare.domain.dto.device.DeviceResponseDTO;
import com.fiap.vinshare.domain.dto.device.RegisterDeviceRequestDTO;
import com.fiap.vinshare.domain.dto.service.ServiceRecordResponseDTO;
import com.fiap.vinshare.domain.dto.vehicle.VehicleResponseDTO;
import com.fiap.vinshare.domain.entities.AppointmentStatus;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.infra.security.SecurityUtils;
import com.fiap.vinshare.service.AppointmentService;
import com.fiap.vinshare.service.DeviceTokenService;
import com.fiap.vinshare.service.MeService;
import com.fiap.vinshare.service.ServiceRecordService;
import com.fiap.vinshare.service.VehicleService;
import com.fiap.vinshare.specs.MeControllerSpecs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController implements MeControllerSpecs {

    private final MeService meService;
    private final VehicleService vehicleService;
    private final DeviceTokenService deviceTokenService;
    private final AppointmentService appointmentService;
    private final ServiceRecordService serviceRecordService;

    @Override
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSingleResponse<MeResponseDTO>> me() {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(meService.buildMe(user)));
    }

    @Override
    @GetMapping("/vehicles")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiSingleResponse<List<VehicleResponseDTO>>> myVehicles() {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(vehicleService.listOwnedBy(user)));
    }

    @Override
    @GetMapping("/services")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiSingleResponse<Page<ServiceRecordResponseDTO>>> myServices(
            @RequestParam(required = false) UUID vehicleId, Pageable pageable) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(
                serviceRecordService.listMine(user, vehicleId, pageable)));
    }

    @Override
    @GetMapping("/appointments")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiSingleResponse<Page<AppointmentResponseDTO>>> myAppointments(
            @RequestParam(required = false) AppointmentStatus status, Pageable pageable) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(
                appointmentService.listMine(user, status, pageable)));
    }

    @Override
    @PostMapping("/devices")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSingleResponse<DeviceResponseDTO>> registerDevice(
            @Valid @RequestBody RegisterDeviceRequestDTO request) {
        var user = SecurityUtils.requireCurrentUser();
        DeviceResponseDTO device = deviceTokenService.register(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSingleResponse.of(device, "Dispositivo registrado"));
    }

    @Override
    @DeleteMapping("/devices/{token}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeDevice(@PathVariable String token) {
        var user = SecurityUtils.requireCurrentUser();
        deviceTokenService.revoke(user, token);
        return ResponseEntity.noContent().build();
    }
}
