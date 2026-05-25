package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.appointment.AppointmentResponseDTO;
import com.fiap.vinshare.domain.dto.appointment.CompleteAppointmentRequestDTO;
import com.fiap.vinshare.domain.dto.appointment.CreateAppointmentRequestDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.infra.security.SecurityUtils;
import com.fiap.vinshare.service.AppointmentService;
import com.fiap.vinshare.specs.AppointmentControllerSpecs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController implements AppointmentControllerSpecs {

    private final AppointmentService appointmentService;

    @Override
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> create(
            @Valid @RequestBody CreateAppointmentRequestDTO request) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSingleResponse.of(appointmentService.create(request, user), "Agendamento criado"));
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> findOne(@PathVariable UUID id) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(appointmentService.getOwned(id, user)));
    }

    @Override
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> cancel(@PathVariable UUID id) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(appointmentService.cancel(id, user)));
    }

    @Override
    @PatchMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> checkIn(@PathVariable UUID id) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(appointmentService.checkIn(id, user)));
    }

    @Override
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> complete(
            @PathVariable UUID id,
            @Valid @RequestBody CompleteAppointmentRequestDTO request) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(
                ApiSingleResponse.of(appointmentService.complete(id, request, user), "Serviço registrado"));
    }
}
