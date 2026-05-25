package com.fiap.vinshare.specs;

import com.fiap.vinshare.domain.dto.service.ServiceRecordResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseNotFound;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Tag(name = "Serviços", description = "Histórico de serviços realizados")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
public interface ServiceRecordControllerSpecs {

    @Operation(summary = "Detalhe de um serviço (do cliente autenticado)")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<ServiceRecordResponseDTO>> findOne(@PathVariable UUID id);
}
