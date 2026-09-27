package com.fiap.vinshare.specs;

import com.fiap.vinshare.domain.dto.loyalty.RewardRequestDTO;
import com.fiap.vinshare.domain.dto.loyalty.RewardResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseBadRequest;
import com.fiap.vinshare.specs.error.ApiResponseForbidden;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseNotFound;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(name = "Prêmios", description = "Catálogo de prêmios de fidelidade: leitura para autenticados, escrita apenas para ADMIN")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
public interface RewardControllerSpecs {

    @Operation(summary = "Detalhe de um prêmio")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<RewardResponseDTO>> findById(@PathVariable UUID id);

    @Operation(summary = "Cadastrar prêmio (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Prêmio criado")
    @ApiResponseBadRequest
    @ApiResponseForbidden
    ResponseEntity<ApiSingleResponse<RewardResponseDTO>> create(@Valid @RequestBody RewardRequestDTO request);

    @Operation(summary = "Atualizar prêmio (ADMIN)")
    @ApiResponseBadRequest
    @ApiResponseForbidden
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<RewardResponseDTO>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody RewardRequestDTO request);

    @Operation(summary = "Desativar prêmio (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Prêmio desativado")
    @ApiResponseForbidden
    @ApiResponseNotFound
    ResponseEntity<Void> deactivate(@PathVariable UUID id);
}
