package com.fiap.vinshare.specs;

import com.fiap.vinshare.domain.dto.appointment.AppointmentResponseDTO;
import com.fiap.vinshare.domain.dto.appointment.CompleteAppointmentRequestDTO;
import com.fiap.vinshare.domain.dto.appointment.CreateAppointmentRequestDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseBadRequest;
import com.fiap.vinshare.specs.error.ApiResponseConflict;
import com.fiap.vinshare.specs.error.ApiResponseForbidden;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseNotFound;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(name = "Agendamentos", description = "Criação, listagem, cancelamento e ciclo de vida do agendamento")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
@ApiResponseForbidden
public interface AppointmentControllerSpecs {

    @Operation(summary = "Criar novo agendamento")
    @ApiResponseBadRequest
    @ApiResponseNotFound
    @ApiResponseConflict
    ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> create(
            @Valid @RequestBody CreateAppointmentRequestDTO request);

    @Operation(summary = "Detalhe de um agendamento")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> findOne(@PathVariable UUID id);

    @Operation(summary = "Cancelar agendamento (cliente)")
    @ApiResponseNotFound
    @ApiResponseConflict
    ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> cancel(@PathVariable UUID id);

    @Operation(summary = "Check-in (analista da concessionária)")
    @ApiResponseNotFound
    @ApiResponseConflict
    ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> checkIn(@PathVariable UUID id);

    @Operation(
            summary = "Concluir agendamento e registrar serviço (analista)",
            description = "Requer header X-Request-Signature (HMAC-SHA256 do body) por exigência da disciplina de Cybersecurity."
    )
    @ApiResponseBadRequest
    @ApiResponseNotFound
    @ApiResponseConflict
    ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> complete(
            @PathVariable UUID id, @Valid @RequestBody CompleteAppointmentRequestDTO request);
}
