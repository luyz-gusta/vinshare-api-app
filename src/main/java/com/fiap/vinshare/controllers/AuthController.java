package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.auth.AuthResponseDTO;
import com.fiap.vinshare.domain.dto.auth.LoginRequestDTO;
import com.fiap.vinshare.domain.dto.auth.RefreshRequestDTO;
import com.fiap.vinshare.domain.dto.auth.RegisterRequestDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.service.AuthService;
import com.fiap.vinshare.specs.AuthControllerSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerSpecs {

    private final AuthService authService;

    @Override
    @PostMapping("/register")
    public ResponseEntity<ApiSingleResponse<AuthResponseDTO>> register(@RequestBody RegisterRequestDTO request) {
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiSingleResponse.of(response, "Cliente registrado com sucesso"));
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<ApiSingleResponse<AuthResponseDTO>> login(@RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(ApiSingleResponse.of(authService.login(request)));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<ApiSingleResponse<AuthResponseDTO>> refresh(@RequestBody RefreshRequestDTO request) {
        return ResponseEntity.ok(ApiSingleResponse.of(authService.refresh(request)));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequestDTO request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
