package com.fiap.vinshare.specs;

import com.fiap.vinshare.domain.dto.auth.AuthResponseDTO;
import com.fiap.vinshare.domain.dto.auth.LoginRequestDTO;
import com.fiap.vinshare.domain.dto.auth.RefreshRequestDTO;
import com.fiap.vinshare.domain.dto.auth.RegisterRequestDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseBadRequest;
import com.fiap.vinshare.specs.error.ApiResponseConflict;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseTooManyRequests;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Autenticação", description = "Login, registro de cliente, refresh e logout")
@ApiResponseInternalServerError
@SecurityRequirements
public interface AuthControllerSpecs {

    @Operation(summary = "Registrar novo cliente")
    @ApiResponseBadRequest
    @ApiResponseConflict
    ResponseEntity<ApiSingleResponse<AuthResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request);

    @Operation(summary = "Autenticar usuário")
    @ApiResponseBadRequest
    @ApiResponseUnauthorized
    @ApiResponseTooManyRequests
    ResponseEntity<ApiSingleResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request);

    @Operation(summary = "Renovar access token (rotaciona o refresh)")
    @ApiResponseBadRequest
    @ApiResponseUnauthorized
    ResponseEntity<ApiSingleResponse<AuthResponseDTO>> refresh(@Valid @RequestBody RefreshRequestDTO request);

    @Operation(summary = "Logout e revogação do refresh token")
    @ApiResponseBadRequest
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDTO request);
}
