package com.fiap.vinshare.infra.errors;

import lombok.RequiredArgsConstructor;
import com.fiap.vinshare.infra.errors.exceptions.BusinessRuleException;
import com.fiap.vinshare.infra.errors.exceptions.BusinessValidationException;
import com.fiap.vinshare.infra.errors.exceptions.DuplicateResourceException;
import com.fiap.vinshare.infra.errors.exceptions.InvalidCredentialsException;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.infra.errors.exceptions.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.Map;

/**
 * Tratamento global de exceções no padrão RFC 7807 (Problem Details).
 *
 * Atende:
 *  - Padrões e Boas Práticas (SOA): status coerente para cada tipo de erro.
 *  - Segurança de Entrada (Cyber): nunca expõe stack trace, SQL ou nome de classe.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final com.fiap.vinshare.infra.security.SecurityEvents securityEvents;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
                .toList();
        ProblemDetail pd = ProblemDetails.of(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos não passaram na validação.", req.getRequestURI());
        pd.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", ex.getMessage(), req);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Parâmetros inválidos",
                "Um ou mais parâmetros não passaram na validação.", req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido",
                "O corpo da requisição está malformado ou contém valores inválidos.", req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente",
                "O parâmetro '%s' é obrigatório.".formatted(ex.getParameterName()), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        String expected = ex.getRequiredType() == null ? "tipo esperado" : ex.getRequiredType().getSimpleName();
        return respond(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "Parâmetro '%s' não pôde ser convertido para %s.".formatted(ex.getName(), expected), req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", "E-mail ou senha incorretos.", req);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex,
                                                                  HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        securityEvents.rejected(req, 403, "regra de método");
        return respond(HttpStatus.FORBIDDEN, "Acesso negado",
                "Você não tem permissão para acessar este recurso.", req);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), req);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoHandlerFoundException ex, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "Recurso não encontrado", "A rota solicitada não existe.", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest req) {
        String supported = ex.getSupportedHttpMethods() == null ? "" : ex.getSupportedHttpMethods().toString();
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido",
                "Método %s não é suportado nesta rota. Aceita: %s".formatted(ex.getMethod(), supported), req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpServletRequest req) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de mídia não suportado",
                "Content-Type %s não é suportado. Aceita: %s".formatted(ex.getContentType(), ex.getSupportedMediaTypes()),
                req);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, "Recurso duplicado", ex.getMessage(), req);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException ex, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, "Regra de negócio violada", ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest req) {
        // Não loga a mensagem do banco: ela pode conter valores (e-mail, CPF).
        log.warn("Violação de integridade em {} {}", req.getMethod(), req.getRequestURI());
        return respond(HttpStatus.CONFLICT, "Conflito de dados",
                "A operação conflita com dados já existentes.", req);
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ProblemDetail> handleBusinessValidation(BusinessValidationException ex,
                                                                  HttpServletRequest req) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "Requisição não processável", ex.getMessage(), req);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetails.of(HttpStatus.TOO_MANY_REQUESTS, "Muitas requisições",
                ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro ao processar a requisição. Tente novamente em instantes.", req);
    }

    private ResponseEntity<ProblemDetail> respond(HttpStatus status, String title, String detail,
                                                  HttpServletRequest req) {
        return ResponseEntity.status(status).body(ProblemDetails.of(status, title, detail, req.getRequestURI()));
    }
}
