package com.fiap.vinshare.specs;

import com.fiap.vinshare.domain.dto.dealership.ServiceTypeResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Tipos de Serviço", description = "Catálogo de tipos de serviço disponíveis")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
public interface ServiceTypeControllerSpecs {

    @Operation(summary = "Listar tipos de serviço")
    ResponseEntity<ApiSingleResponse<List<ServiceTypeResponseDTO>>> list();
}
