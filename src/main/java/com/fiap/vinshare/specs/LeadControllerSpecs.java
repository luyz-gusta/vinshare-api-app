package com.fiap.vinshare.specs;

import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.fiap.vinshare.domain.dto.lead.LeadActionRequestDTO;
import com.fiap.vinshare.domain.dto.lead.LeadActionResponseDTO;
import com.fiap.vinshare.domain.dto.lead.LeadResponseDTO;
import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import com.fiap.vinshare.domain.entities.LeadHealthStatus;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseBadRequest;
import com.fiap.vinshare.specs.error.ApiResponseForbidden;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseNotFound;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Leads", description = "Leads gerados a partir da segmentação (apenas analista/admin)")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
@ApiResponseForbidden
public interface LeadControllerSpecs {

    @Operation(summary = "Listar leads (por padrão, segmentos em risco). Aceita filtros por segmento e/ou status derivado do riskScore.")
    @ApiResponse(responseCode = "200", description = "Sucesso")
    ResponseEntity<ApiSingleResponse<Page<LeadResponseDTO>>> list(
            @RequestParam(required = false) CustomerSegmentType segment,
            @RequestParam(required = false) LeadHealthStatus status,
            @ParameterObject Pageable pageable);

    @Operation(summary = "Detalhe do lead (segmento atual do cliente)")
    @ApiResponse(responseCode = "200", description = "Sucesso")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<LeadResponseDTO>> findOne(@PathVariable UUID customerId);

    @Operation(summary = "Registrar ação de contato com o lead (analista da concessionária de relacionamento)")
    @ApiResponse(responseCode = "201", description = "Ação registrada")
    @ApiResponseBadRequest
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<LeadActionResponseDTO>> triggerAction(
            @PathVariable UUID customerId,
            @Valid @RequestBody LeadActionRequestDTO request);
}
