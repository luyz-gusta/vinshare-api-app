package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.chat.ChatMessageResponseDTO;
import com.fiap.vinshare.domain.dto.chat.ChatSessionResponseDTO;
import com.fiap.vinshare.domain.dto.chat.SendMessageRequestDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.infra.security.SecurityUtils;
import com.fiap.vinshare.service.ChatService;
import com.fiap.vinshare.specs.ChatControllerSpecs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat/sessions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatController implements ChatControllerSpecs {

    private final ChatService chatService;

    @Override
    @PostMapping
    public ResponseEntity<ApiSingleResponse<ChatSessionResponseDTO>> openSession() {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSingleResponse.of(chatService.openSession(user), "Sessão aberta"));
    }

    @Override
    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<ApiSingleResponse<ChatMessageResponseDTO>> sendMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SendMessageRequestDTO request) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(chatService.sendMessage(sessionId, request, user)));
    }

    @Override
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<ApiSingleResponse<List<ChatMessageResponseDTO>>> listMessages(@PathVariable UUID sessionId) {
        var user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiSingleResponse.of(chatService.listMessages(sessionId, user)));
    }
}
