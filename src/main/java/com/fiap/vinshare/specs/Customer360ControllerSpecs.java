package com.fiap.vinshare.specs;

import com.fiap.vinshare.domain.dto.customer.Customer360DTO;
import com.fiap.vinshare.domain.dto.customer.TimelineEventDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseForbidden;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseNotFound;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Cliente 360", description = "Visão consolidada do cliente para o analista")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
@ApiResponseForbidden
public interface Customer360ControllerSpecs {

    @Operation(summary = "Visão 360 do cliente")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<Customer360DTO>> get360(@PathVariable UUID customerId);

    @Operation(summary = "Linha do tempo de eventos do cliente")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<List<TimelineEventDTO>>> timeline(
            @PathVariable UUID customerId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to);
}
