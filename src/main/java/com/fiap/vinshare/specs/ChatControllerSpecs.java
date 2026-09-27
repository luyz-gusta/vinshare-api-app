package com.fiap.vinshare.specs;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.fiap.vinshare.domain.dto.chat.ChatMessageResponseDTO;
import com.fiap.vinshare.domain.dto.chat.ChatSessionResponseDTO;
import com.fiap.vinshare.domain.dto.chat.SendMessageRequestDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseBadRequest;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseNotFound;
import com.fiap.vinshare.specs.error.ApiResponseTooManyRequests;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Tag(name = "Chat", description = "Suporte conversacional com IA (Gemini)")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
public interface ChatControllerSpecs {

    @Operation(summary = "Abrir sessão de chat")
    @ApiResponse(responseCode = "201", description = "Sessão criada")
    ResponseEntity<ApiSingleResponse<ChatSessionResponseDTO>> openSession();

    @Operation(summary = "Enviar mensagem na sessão (rate limit de 20/min)")
    @ApiResponse(responseCode = "201", description = "Resposta do assistente criada")
    @ApiResponseBadRequest
    @ApiResponseNotFound
    @ApiResponseTooManyRequests
    ResponseEntity<ApiSingleResponse<ChatMessageResponseDTO>> sendMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SendMessageRequestDTO request);

    @Operation(summary = "Histórico de mensagens da sessão")
    @ApiResponseNotFound
    ResponseEntity<ApiSingleResponse<List<ChatMessageResponseDTO>>> listMessages(@PathVariable UUID sessionId);
}
