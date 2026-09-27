package com.fiap.vinshare.infra.security;

import com.fiap.vinshare.infra.errors.ProblemDetailsWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 em ProblemDetail com o desafio WWW-Authenticate da RFC 6750, para o
 * cliente saber se deve renovar o token (expirado) ou refazer o login.
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailsWriter writer;
    private final SecurityEvents securityEvents;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object error = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        String detail;
        String challenge;
        if ("expired".equals(error)) {
            detail = "Token expirado. Renove o acesso em /auth/refresh.";
            challenge = "Bearer error=\"invalid_token\", error_description=\"token expired\"";
        } else if ("invalid".equals(error)) {
            detail = "Token inválido.";
            challenge = "Bearer error=\"invalid_token\", error_description=\"invalid token\"";
        } else {
            detail = "Autenticação obrigatória. Envie o header Authorization: Bearer <token>.";
            challenge = "Bearer";
        }
        securityEvents.rejected(request, 401, error == null ? "sem token" : error.toString());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge);
        writer.write(request, response, HttpStatus.UNAUTHORIZED, "Não autenticado", detail);
    }
}
