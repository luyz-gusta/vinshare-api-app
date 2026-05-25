package com.fiap.vinshare.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cliente para o serviço de push da Expo (https://exp.host/--/api/v2/push/send).
 * Mantém o access token no backend. Sem token configurado, apenas loga
 * (modo demo), para o fluxo funcionar localmente sem dependência externa.
 */
@Slf4j
@Component
public class ExpoPushClient {

    private static final String PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final String accessToken;

    public ExpoPushClient(@Value("${integrations.expo.access-token:}") String accessToken) {
        this.accessToken = accessToken;
    }

    public boolean isEnabled() {
        return accessToken != null && !accessToken.isBlank();
    }

    /**
     * Envia uma notificação para uma lista de Expo Push Tokens.
     * @return quantidade de mensagens efetivamente enviadas (ou simuladas).
     */
    public int send(List<String> tokens, String title, String body, Map<String, Object> data) {
        if (tokens == null || tokens.isEmpty()) return 0;

        if (!isEnabled()) {
            log.info("[push simulado] {} dispositivo(s) | título='{}' corpo='{}'", tokens.size(), title, body);
            return tokens.size();
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (String token : tokens) {
            messages.add(Map.of(
                    "to", token,
                    "title", title,
                    "body", body,
                    "data", data == null ? Map.of() : data,
                    "sound", "default"));
        }

        try {
            RestClient.create().post()
                    .uri(PUSH_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(messages)
                    .retrieve()
                    .toBodilessEntity();
            return messages.size();
        } catch (RestClientException ex) {
            log.error("Falha ao enviar push via Expo: {}", ex.getMessage());
            return 0;
        }
    }
}
