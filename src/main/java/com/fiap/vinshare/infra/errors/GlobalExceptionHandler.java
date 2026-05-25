package com.fiap.vinshare.infra.errors;

import com.fiap.vinshare.infra.errors.exceptions.BusinessRuleException;
import com.fiap.vinshare.infra.errors.exceptions.DuplicateResourceException;
import com.fiap.vinshare.infra.errors.exceptions.InvalidCredentialsException;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Tratamento global de exceções no padrão RFC 7807 (Problem Details).
 *
 * Atende:
 *  - Padrões e Boas Práticas (SOA): tratamento adequado de erros.
 *  - Segurança de Entrada (Cyber): nunca expõe stack trace ou estrutura interna.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI BASE = URI.create("https://api.fordvinshare.fiap/errors/");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
                .toList();

        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos não passaram na validação.", req);
        pd.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", ex.getMessage(), req);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        log.warn("Falha de autenticação em {}", req.getRequestURI());
        ProblemDetail pd = build(HttpStatus.UNAUTHORIZED, "Credenciais inválidas",
                "E-mail ou senha incorretos.", req);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.FORBIDDEN, "Acesso negado",
                "Você não tem permissão para acessar este recurso.", req);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest req) {
        log.warn("Credenciais inválidas em {}", req.getRequestURI());
        ProblemDetail pd = build(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", ex.getMessage(), req);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), req);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.CONFLICT, "Recurso duplicado", ex.getMessage(), req);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.CONFLICT, "Regra de negócio violada", ex.getMessage(), req);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoHandlerFoundException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.NOT_FOUND, "Recurso não encontrado",
                "A rota solicitada não existe.", req);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String expected = ex.getRequiredType() == null ? "tipo esperado" : ex.getRequiredType().getSimpleName();
        String detail = String.format("Parâmetro '%s' com valor '%s' não pôde ser convertido para %s.",
                ex.getName(), ex.getValue(), expected);
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "Parâmetro inválido", detail, req);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        String supported = ex.getSupportedHttpMethods() == null
                ? ""
                : ex.getSupportedHttpMethods().toString();
        String detail = String.format("Método %s não é suportado nesta rota. Aceita: %s",
                ex.getMethod(), supported);
        ProblemDetail pd = build(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido", detail, req);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(pd);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        String detail = String.format("Content-Type %s não é suportado. Aceita: %s",
                ex.getContentType(), ex.getSupportedMediaTypes());
        ProblemDetail pd = build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de mídia não suportado", detail, req);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        ProblemDetail pd = build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro ao processar a requisição. Tente novamente em instantes.", req);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    private ProblemDetail build(HttpStatus status, String title, String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(BASE.resolve(status.value() + ""));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }
}
