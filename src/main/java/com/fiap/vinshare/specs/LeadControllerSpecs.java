package com.fiap.vinshare.specs;

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
    ResponseEntity<ApiSingleResponse<Page<LeadResponseDTO>>> list(
            @RequestParam(required = false) CustomerSegmentType segment,
            @RequestParam(required = false) LeadHealthStatus status,
            Pageable pageable);

    @Operation(summary = "Detalhe do lead (segmento atual do cliente)")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<LeadResponseDTO>> findOne(@PathVariable UUID customerId);

    @Operation(
            summary = "Disparar ação de lead",
            description = "Requer X-Request-Signature (HMAC) para integridade do payload (Cyber, frente 3)."
    )
    @ApiResponseBadRequest
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<LeadActionResponseDTO>> triggerAction(
            @PathVariable UUID customerId,
            @Valid @RequestBody LeadActionRequestDTO request);
}
