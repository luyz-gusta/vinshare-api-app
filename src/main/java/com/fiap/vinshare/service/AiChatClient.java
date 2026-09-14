package com.fiap.vinshare.service;

import com.fiap.vinshare.infra.security.AiChatProperties;
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
 * Cliente HTTP para a Gemini API (Google). Mantém a chave no backend (Cyber,
 * frente 2). Quando a chave não estiver configurada, retorna respostas mock
 * para o demo funcionar localmente.
 *
 * Trocado de Anthropic (Claude) para Google Gemini em 2026-09 — o tier
 * gratuito do Gemini cobre o volume deste chat sem custo. A assinatura
 * pública ({@link #complete}) não mudou, então {@code ChatService} não
 * precisou de nenhum ajuste além do tipo injetado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatClient {

    private final AiChatProperties props;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-goog-api-key", props.getApiKey() == null ? "" : props.getApiKey())
                .build();
    }

    /**
     * @param systemPrompt instrução de sistema (persona + contexto do cliente)
     * @param messages     histórico no formato {role: "user"|"assistant", content: "..."},
     *                     mesmo formato que o ChatService já monta para a Anthropic —
     *                     "assistant" é remapeado para "model" abaixo, único valor de
     *                     role que a Gemini API aceita para turnos do modelo.
     */
    public String complete(String systemPrompt, List<Map<String, String>> messages) {
        if (!props.isEnabled()) {
            log.warn("Gemini API desativada (sem GEMINI_API_KEY). Retornando resposta simulada.");
            return mockReply(messages);
        }
        try {
            List<Map<String, Object>> contents = messages.stream()
                    .map(m -> Map.<String, Object>of(
                            "role", "assistant".equals(m.get("role")) ? "model" : "user",
                            "parts", List.of(Map.of("text", m.get("content")))))
                    .toList();

            // thinkingBudget=0 desativa o raciocínio interno do modelo: sem isso, um
            // maxOutputTokens baixo é consumido inteiro pensando e a resposta visível
            // sai vazia (finishReason=MAX_TOKENS) — confirmado ao vivo contra a API.
            // Desnecessário para um chat de suporte com respostas curtas e diretas.
            Map<String, Object> body = Map.of(
                    "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                    "contents", contents,
                    "generationConfig", Map.of(
                            "maxOutputTokens", props.getMaxTokens(),
                            "thinkingConfig", Map.of("thinkingBudget", 0)
                    )
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = client().post()
                    .uri("/v1beta/models/{model}:generateContent", props.getModel())
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return "Não consegui processar a resposta. Tente novamente.";

            Object candidates = response.get("candidates");
            if (candidates instanceof List<?> candidateList && !candidateList.isEmpty()
                    && candidateList.get(0) instanceof Map<?, ?> firstCandidate
                    && firstCandidate.get("content") instanceof Map<?, ?> content
                    && content.get("parts") instanceof List<?> parts && !parts.isEmpty()
                    && parts.get(0) instanceof Map<?, ?> firstPart
                    && firstPart.get("text") instanceof String text) {
                return text;
            }
            log.warn("Resposta da Gemini API em formato inesperado: {}", response.keySet());
            return "Desculpe, não consegui formular uma resposta agora.";
        } catch (RestClientException ex) {
            log.error("Erro na Gemini API: {}", ex.getMessage());
            return "Estou com dificuldade de me conectar ao suporte inteligente. Tente novamente em alguns minutos.";
        }
    }

    private String mockReply(List<Map<String, String>> messages) {
        String last = messages.isEmpty() ? "" : String.valueOf(messages.get(messages.size() - 1).get("content"));
        return "Olá! Posso ajudar você com a sua Ford. Sua pergunta: \"%s\". "
                .formatted(last)
                + "Para o demo local, esta é uma resposta simulada. Configure GEMINI_API_KEY para respostas reais.";
    }
}
