package com.fiap.vinshare.specs;

import org.springdoc.core.annotations.ParameterObject;
import com.fiap.vinshare.domain.dto.segment.CustomerSegmentDTO;
import com.fiap.vinshare.domain.dto.segment.SegmentCustomerDTO;
import com.fiap.vinshare.domain.dto.segment.SegmentDistributionResponseDTO;
import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseForbidden;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseNotFound;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Tag(name = "Segmentação", description = "Predições de IA por cliente (apenas analista/admin)")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
@ApiResponseForbidden
public interface SegmentControllerSpecs {

    @Operation(summary = "Distribuição dos segmentos no momento atual (envelope com totalCustomers e buckets)")
    @ApiResponse(responseCode = "200", description = "Sucesso")
    ResponseEntity<ApiSingleResponse<SegmentDistributionResponseDTO>> distribution();

    @Operation(summary = "Clientes do segmento informado (lista navegável com nome, CPF mascarado, última visita e LTV estimado)")
    @ApiResponse(responseCode = "200", description = "Sucesso")
    ResponseEntity<ApiSingleResponse<Page<SegmentCustomerDTO>>> bySegment(
            @PathVariable CustomerSegmentType segment, @ParameterObject Pageable pageable);

    @Operation(summary = "Segmento atual de um cliente")
    @ApiResponse(responseCode = "200", description = "Sucesso")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<CustomerSegmentDTO>> getCustomerSegment(@PathVariable UUID customerId);
}
