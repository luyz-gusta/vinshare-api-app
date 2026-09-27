package com.fiap.vinshare.infra.errors;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escreve ProblemDetail direto na resposta. Usado por quem roda antes do
 * DispatcherServlet (filtros de segurança e rate limit), onde o
 * GlobalExceptionHandler não alcança.
 */
@Component
@RequiredArgsConstructor
public class ProblemDetailsWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String title, String detail) throws IOException {
        ProblemDetail pd = ProblemDetails.of(status, title, detail, request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), pd);
    }
}
