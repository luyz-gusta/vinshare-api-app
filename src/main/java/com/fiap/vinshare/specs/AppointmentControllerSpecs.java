package com.fiap.vinshare.specs;

import com.fiap.vinshare.specs.error.ApiResponseUnprocessableEntity;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @ApiResponse(responseCode = "201", description = "Agendamento criado; header Location aponta para o recurso")
    @ApiResponseUnprocessableEntity
    @ApiResponseBadRequest
    @ApiResponseNotFound
    @ApiResponseConflict
    ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> create(
            @Valid @RequestBody CreateAppointmentRequestDTO request);

    @Operation(summary = "Detalhe de um agendamento")
    @ApiResponse(responseCode = "200", description = "Sucesso")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> findOne(@PathVariable UUID id);

    @Operation(summary = "Cancelar agendamento (cliente)")
    @ApiResponse(responseCode = "200", description = "Sucesso")
    @ApiResponseNotFound
    @ApiResponseConflict
    ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> cancel(@PathVariable UUID id);

    @Operation(summary = "Check-in (analista da concessionária)")
    @ApiResponse(responseCode = "200", description = "Sucesso")
    @ApiResponseNotFound
    @ApiResponseConflict
    ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> checkIn(@PathVariable UUID id);

    @Operation(summary = "Concluir agendamento e registrar serviço (analista da concessionária)")
    @ApiResponse(responseCode = "200", description = "Sucesso")
    @ApiResponseBadRequest
    @ApiResponseNotFound
    @ApiResponseConflict
    ResponseEntity<ApiSingleResponse<AppointmentResponseDTO>> complete(
            @PathVariable UUID id, @Valid @RequestBody CompleteAppointmentRequestDTO request);
}
