package com.fiap.vinshare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração do fluxo de autenticação e do controle de acesso por papel.
 * Sobe um PostgreSQL real via Testcontainers e aplica as migrations Flyway.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String accessToken;
    private static String refreshToken;

    private static final String REGISTER_BODY = """
            {
              "fullName": "Cliente Teste",
              "email": "test.client@email.com",
              "password": "senha12345",
              "cpf": "39053344705",
              "phone": "+5511999998888",
              "lgpdConsent": true
            }
            """;

    private static final String LOGIN_BODY = """
            {"email": "test.client@email.com", "password": "senha12345"}
            """;

    @Test
    @Order(1)
    void registraCliente() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("accessToken").asText()).isNotBlank();
    }

    @Test
    @Order(2)
    void loginRetornaTokens() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        accessToken = data.get("accessToken").asText();
        refreshToken = data.get("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(data.get("role").asText()).isEqualTo("CLIENT");
    }

    @Test
    @Order(3)
    void meComTokenRetornaUsuario() throws Exception {
        mockMvc.perform(get("/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    void meSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void clienteEmEndpointDeAnalistaRetorna403() throws Exception {
        mockMvc.perform(get("/analytics/kpis").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    void refreshGeraNovoAccessToken() throws Exception {
        String body = "{\"refreshToken\": \"" + refreshToken + "\"}";
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }
}
