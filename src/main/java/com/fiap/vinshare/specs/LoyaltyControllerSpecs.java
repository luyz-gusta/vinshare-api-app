package com.fiap.vinshare.specs;

import com.fiap.vinshare.domain.dto.loyalty.LoyaltyBalanceDTO;
import com.fiap.vinshare.domain.dto.loyalty.LoyaltyTransactionDTO;
import com.fiap.vinshare.domain.dto.loyalty.RedeemRequestDTO;
import com.fiap.vinshare.domain.dto.loyalty.RedeemResponseDTO;
import com.fiap.vinshare.domain.dto.loyalty.RewardResponseDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseBadRequest;
import com.fiap.vinshare.specs.error.ApiResponseConflict;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseNotFound;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Fidelidade", description = "Programa de pontos e resgates")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
public interface LoyaltyControllerSpecs {

    @Operation(summary = "Saldo de pontos do cliente autenticado")
    ResponseEntity<ApiSingleResponse<LoyaltyBalanceDTO>> balance();

    @Operation(summary = "Extrato de transações de pontos")
    ResponseEntity<ApiSingleResponse<Page<LoyaltyTransactionDTO>>> transactions(Pageable pageable);

    @Operation(summary = "Catálogo de prêmios disponíveis")
    ResponseEntity<ApiSingleResponse<List<RewardResponseDTO>>> rewards();

    @Operation(summary = "Resgatar prêmio")
    @ApiResponseBadRequest
    @ApiResponseNotFound
    @ApiResponseConflict
    ResponseEntity<ApiSingleResponse<RedeemResponseDTO>> redeem(@Valid @RequestBody RedeemRequestDTO request);
}
