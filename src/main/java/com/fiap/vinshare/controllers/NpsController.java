package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.nps.CreateNpsRequestDTO;
import com.fiap.vinshare.domain.dto.nps.NpsResponseDTO;
import com.fiap.vinshare.domain.dto.nps.PendingSurveyDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.infra.security.SecurityUtils;
import com.fiap.vinshare.service.NpsService;
import com.fiap.vinshare.specs.NpsControllerSpecs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class NpsController implements NpsControllerSpecs {

    private final NpsService npsService;

    @Override
    @GetMapping("/me/surveys/pending")
    public ResponseEntity<ApiSingleResponse<List<PendingSurveyDTO>>> listPending() {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(npsService.listPending(user)));
    }

    @Override
    @PostMapping("/services/{serviceId}/nps")
    public ResponseEntity<ApiSingleResponse<NpsResponseDTO>> submit(
            @PathVariable UUID serviceId,
            @Valid @RequestBody CreateNpsRequestDTO request) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSingleResponse.of(npsService.submit(serviceId, request, user), "NPS registrado"));
    }

    @Override
    @GetMapping("/services/{serviceId}/nps")
    public ResponseEntity<ApiSingleResponse<NpsResponseDTO>> getByServiceId(@PathVariable UUID serviceId) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(npsService.getByServiceId(serviceId, user)));
    }
}
