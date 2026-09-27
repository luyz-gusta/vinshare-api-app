package com.fiap.vinshare.specs;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.fiap.vinshare.domain.dto.nps.CreateNpsRequestDTO;
import com.fiap.vinshare.domain.dto.nps.NpsResponseDTO;
import com.fiap.vinshare.domain.dto.nps.PendingSurveyDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseBadRequest;
import com.fiap.vinshare.specs.error.ApiResponseConflict;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseNotFound;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Tag(name = "NPS", description = "Pesquisas de satisfação pós-serviço")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
public interface NpsControllerSpecs {

    @Operation(summary = "Listar pesquisas pendentes do cliente")
    @ApiResponse(responseCode = "200", description = "Sucesso")
    ResponseEntity<ApiSingleResponse<List<PendingSurveyDTO>>> listPending();

    @Operation(summary = "Enviar avaliação NPS para um serviço")
    @ApiResponse(responseCode = "201", description = "Avaliação registrada")
    @ApiResponseBadRequest
    @ApiResponseNotFound
    @ApiResponseConflict
    ResponseEntity<ApiSingleResponse<NpsResponseDTO>> submit(
            @PathVariable UUID serviceId,
            @Valid @RequestBody CreateNpsRequestDTO request);

    @Operation(summary = "Consultar NPS de um serviço")
    @ApiResponse(responseCode = "200", description = "Sucesso")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<NpsResponseDTO>> getByServiceId(@PathVariable UUID serviceId);
}
