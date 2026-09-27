package com.fiap.vinshare;

import com.fiap.vinshare.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityHeadersTest extends IntegrationTest {

    private static final String UUID_REGEX =
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    @Test
    void respostaDaApiTemHstsECspRestritiva() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void documentacaoSwaggerNaoRecebeCspRestritiva() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }

    @Test
    void correlationIdInvalidoESubstituidoPorUuid() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "<script>alert(1)</script>"))
                .andExpect(header().string("X-Request-Id", matchesPattern(UUID_REGEX)));
    }

    @Test
    void correlationIdValidoEPropagado() throws Exception {
        String id = UUID.randomUUID().toString();
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", id))
                .andExpect(header().string("X-Request-Id", id));
    }

    @Test
    void corsNaoLiberaCredenciais() throws Exception {
        mockMvc.perform(options("/me")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
}
