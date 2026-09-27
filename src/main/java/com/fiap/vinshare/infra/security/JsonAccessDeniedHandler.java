package com.fiap.vinshare.infra.security;

import com.fiap.vinshare.infra.errors.ProblemDetailsWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 403 em ProblemDetail para negações decididas na cadeia de filtros (regras de URL). */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailsWriter writer;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("403 em {} {}", request.getMethod(), request.getRequestURI());
        writer.write(request, response, HttpStatus.FORBIDDEN, "Acesso negado",
                "Você não tem permissão para acessar este recurso.");
    }
}
