package com.fiap.vinshare.infra.security;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ClaudeProperties {

    @Value("${integrations.claude.api-key:}")
    private String apiKey;

    @Value("${integrations.claude.model:claude-sonnet-4-6}")
    private String model;

    @Value("${integrations.claude.max-tokens:1024}")
    private int maxTokens;

    @Value("${integrations.claude.base-url:https://api.anthropic.com}")
    private String baseUrl;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
