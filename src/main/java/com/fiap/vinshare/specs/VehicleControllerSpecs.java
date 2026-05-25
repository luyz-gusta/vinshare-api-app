package com.fiap.vinshare.specs;

import com.fiap.vinshare.domain.dto.vehicle.MaintenanceAlertResponseDTO;
import com.fiap.vinshare.domain.dto.vehicle.UpdateOdometerRequestDTO;
import com.fiap.vinshare.domain.dto.vehicle.VehicleResponseDTO;
import com.fiap.vinshare.domain.dto.vehicle.WarrantyResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseBadRequest;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Veículos", description = "Garantia, alertas de manutenção e hodômetro")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
@ApiResponseForbidden
@ApiResponseNotFound
public interface VehicleControllerSpecs {

    @Operation(summary = "Status da garantia de um veículo")
    ResponseEntity<ApiSingleResponse<WarrantyResponseDTO>> warranty(@PathVariable UUID vehicleId);

    @Operation(summary = "Alertas de manutenção ativos do veículo")
    ResponseEntity<ApiSingleResponse<List<MaintenanceAlertResponseDTO>>> alerts(@PathVariable UUID vehicleId);

    @Operation(summary = "Atualizar quilometragem informada pelo cliente")
    @ApiResponseBadRequest
    ResponseEntity<ApiSingleResponse<VehicleResponseDTO>> updateOdometer(
            @PathVariable UUID vehicleId, @Valid @RequestBody UpdateOdometerRequestDTO request);
}
