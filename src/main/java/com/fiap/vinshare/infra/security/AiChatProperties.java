package com.fiap.vinshare.infra.security;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do provedor de IA usado no chat (Google Gemini API).
 * O nome de modelo padrão é o do tier gratuito do Google AI Studio no
 * momento desta troca (2026-09) — confira em https://ai.google.dev/pricing
 * antes de configurar em produção, caso o catálogo de modelos tenha mudado.
 */
@Getter
@Configuration
public class AiChatProperties {

    @Value("${integrations.gemini.api-key:}")
    private String apiKey;

    @Value("${integrations.gemini.model:gemini-3.6-flash}")
    private String model;

    @Value("${integrations.gemini.max-tokens:1024}")
    private int maxTokens;

    @Value("${integrations.gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
