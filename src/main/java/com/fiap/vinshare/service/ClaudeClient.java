package com.fiap.vinshare.service;

import com.fiap.vinshare.infra.security.ClaudeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para a Claude API (Anthropic).
 * Mantém a chave no backend (Cyber, frente 2). Quando a chave não estiver
 * configurada, retorna respostas mock para o demo funcionar localmente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeClient {

    private final ClaudeProperties props;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("x-api-key", props.getApiKey() == null ? "" : props.getApiKey())
                .build();
    }

    public String complete(String systemPrompt, List<Map<String, String>> messages) {
        if (!props.isEnabled()) {
            log.warn("Claude API desativada (sem CLAUDE_API_KEY). Retornando resposta simulada.");
            return mockReply(messages);
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", props.getModel(),
                    "max_tokens", props.getMaxTokens(),
                    "system", systemPrompt,
                    "messages", messages
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client().post()
                    .uri("/v1/messages")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return "Não consegui processar a resposta. Tente novamente.";
            Object content = response.get("content");
            if (content instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof Map<?, ?> first
                    && first.get("text") instanceof String text) {
                return text;
            }
            log.warn("Resposta da Claude API em formato inesperado: {}", response.keySet());
            return "Desculpe, não consegui formular uma resposta agora.";
        } catch (RestClientException ex) {
            log.error("Erro na Claude API: {}", ex.getMessage());
            return "Estou com dificuldade de me conectar ao suporte inteligente. Tente novamente em alguns minutos.";
        }
    }

    private String mockReply(List<Map<String, String>> messages) {
        String last = messages.isEmpty() ? "" : String.valueOf(messages.get(messages.size() - 1).get("content"));
        return "Olá! Posso ajudar você com a sua Ford. Sua pergunta: \"%s\". "
                .formatted(last)
                + "Para o demo local, esta é uma resposta simulada. Configure CLAUDE_API_KEY para respostas reais.";
    }
}
