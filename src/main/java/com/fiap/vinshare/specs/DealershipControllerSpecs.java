package com.fiap.vinshare.specs;

import com.fiap.vinshare.domain.dto.dealership.DealershipAvailabilityResponseDTO;
import com.fiap.vinshare.domain.dto.dealership.DealershipResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseBadRequest;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseNotFound;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Concessionárias", description = "Localizador, detalhes e slots de disponibilidade")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
public interface DealershipControllerSpecs {

    @Operation(summary = "Buscar concessionárias próximas (Haversine)")
    @ApiResponseBadRequest
    ResponseEntity<ApiSingleResponse<List<DealershipResponseDTO>>> nearby(
            @Parameter(description = "Latitude do usuário") @RequestParam BigDecimal lat,
            @Parameter(description = "Longitude do usuário") @RequestParam BigDecimal lng,
            @Parameter(description = "Raio em km, padrão 25") @RequestParam(required = false) Double radiusKm,
            @Parameter(description = "Filtro opcional por tipo de serviço") @RequestParam(required = false) String service);

    @Operation(summary = "Detalhe da concessionária")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<DealershipResponseDTO>> findById(@PathVariable UUID id);

    @Operation(summary = "Slots de disponibilidade para agendamento")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<DealershipAvailabilityResponseDTO>> availability(
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String serviceType);
}
