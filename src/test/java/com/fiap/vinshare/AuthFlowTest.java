package com.fiap.vinshare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.infra.security.JwtService;
import com.fiap.vinshare.support.IntegrationTest;
import com.fiap.vinshare.support.TestData;
import com.fiap.vinshare.support.TestFixtures;
import org.junit.jupiter.api.Test;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fluxo de autenticação (cadastro, login, refresh) e controle de acesso básico. */
class AuthFlowTest extends IntegrationTest {

    private String registerBody(String email, String cpf) {
        return """
                {"fullName":"Cliente Teste","email":"%s","password":"%s",
                 "cpf":"%s","phone":"+5511999998888","lgpdConsent":true}
                """.formatted(email, TestFixtures.PASSWORD, cpf);
    }

    private JsonNode login(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("data");
    }

    @Test
    void cadastroDeClienteRetorna201ComTokens() throws Exception {
        var result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(TestData.uniqueEmail("novo"), TestData.randomCpf())))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(json(result).at("/data/accessToken").asText()).isNotBlank();
    }

    @Test
    void loginRetornaTokensEPapel() throws Exception {
        Customer customer = fixtures.customer();
        JsonNode data = login(customer.getUser().getEmail(), TestFixtures.PASSWORD);
        assertThat(data.get("accessToken").asText()).isNotBlank();
        assertThat(data.get("refreshToken").asText()).isNotBlank();
        assertThat(data.get("role").asText()).isEqualTo("CLIENT");
    }

    @Test
    void meComTokenRetorna200() throws Exception {
        Customer customer = fixtures.customer();
        mockMvc.perform(get("/me").header("Authorization", bearer(fixtures.tokenFor(customer.getUser()))))
                .andExpect(status().isOk());
    }

    @Test
    void meSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void clienteEmEndpointDeAnalistaRetorna403() throws Exception {
        Customer customer = fixtures.customer();
        mockMvc.perform(get("/analytics/kpis").header("Authorization", bearer(fixtures.tokenFor(customer.getUser()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshGeraNovoAccessToken() throws Exception {
        Customer customer = fixtures.customer();
        String refresh = login(customer.getUser().getEmail(), TestFixtures.PASSWORD).get("refreshToken").asText();
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void loginComSenhaErradaRetorna401() throws Exception {
        Customer customer = fixtures.customer();
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"senha-errada-123\"}"
                                .formatted(customer.getUser().getEmail())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroComEmailDuplicadoRetorna409() throws Exception {
        Customer customer = fixtures.customer();
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(customer.getUser().getEmail(), TestData.randomCpf())))
                .andExpect(status().isConflict());
    }

    @Test
    void cadastroComCamposInvalidosRetorna400ComListaDeErros() throws Exception {
        var result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"nao-e-email\",\"password\":\"123\",\"cpf\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(json(result).get("errors")).isNotEmpty();
    }

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private int refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    private String rotate(String refreshToken) throws Exception {
        var rotated = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return json(rotated).at("/data/refreshToken").asText();
    }

    @Test
    void refreshReutilizadoRevogaTodaASessao() throws Exception {
        Customer customer = fixtures.customer();
        String r1 = login(customer.getUser().getEmail(), TestFixtures.PASSWORD).get("refreshToken").asText();
        String r2 = rotate(r1);
        // Rotação antiga: fora da tolerância para renovações simultâneas do app.
        jdbcTemplate.update("UPDATE refresh_tokens SET revoked_at = now() - interval '5 minutes' WHERE token_hash = ?",
                jwtService.hashRefreshToken(r1));

        assertThat(refresh(r1)).isEqualTo(401);   // reuso do token antigo
        assertThat(refresh(r2)).isEqualTo(401);   // a família inteira foi revogada
    }

    @Test
    void refreshRepetidoLogoAposARotacaoNaoDerrubaASessao() throws Exception {
        // O app renovou duas vezes em paralelo: a segunda chamada chega com o token que a primeira acabou de trocar.
        Customer customer = fixtures.customer();
        String r1 = login(customer.getUser().getEmail(), TestFixtures.PASSWORD).get("refreshToken").asText();
        String r2 = rotate(r1);

        assertThat(refresh(r1)).isEqualTo(401);   // o token antigo não renova de novo
        assertThat(refresh(r2)).isEqualTo(200);   // e a sessão continua de pé
    }

    @Test
    void logoutSemAccessTokenRevogaRefresh() throws Exception {
        Customer customer = fixtures.customer();
        String r1 = login(customer.getUser().getEmail(), TestFixtures.PASSWORD).get("refreshToken").asText();

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + r1 + "\"}"))
                .andExpect(status().isNoContent());
        assertThat(refresh(r1)).isEqualTo(401);
    }

    @Test
    void tokenAntigoSemAudienceRecebe401MasRefreshFunciona() throws Exception {
        Customer customer = fixtures.customer();
        String refreshToken = login(customer.getUser().getEmail(), TestFixtures.PASSWORD).get("refreshToken").asText();
        // Simula um access token emitido antes do deploy (sem claim aud).
        String legacy = Jwts.builder()
                .subject(customer.getUser().getId().toString())
                .issuer("ford-vinshare-api")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(10))))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/me").header("Authorization", bearer(legacy)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("invalid_token")));
        assertThat(refresh(refreshToken)).isEqualTo(200);
    }

    @Test
    void bloqueiaLoginDoEmailAposCincoFalhas() throws Exception {
        Customer customer = fixtures.customer();
        String email = customer.getUser().getEmail();
        String wrong = "{\"email\":\"%s\",\"password\":\"senha-errada-123\"}".formatted(email);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrong))
                    .andExpect(status().isUnauthorized());
        }
        // Mesmo com a senha certa, o e-mail fica bloqueado durante a janela.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, TestFixtures.PASSWORD)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void cadastroSemConsentimentoLgpdRetorna400() throws Exception {
        String body = """
                {"fullName":"Sem Consentimento","email":"%s","password":"%s",
                 "cpf":"%s","lgpdConsent":false}
                """.formatted(TestData.uniqueEmail("sem"), TestFixtures.PASSWORD, TestData.randomCpf());
        var result = mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(json(result).get("errors").toString()).contains("lgpdConsent");
    }

    @Test
    void cadastroComCpfComDigitoVerificadorInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(TestData.uniqueEmail("cpf"), "12345678900")))
                .andExpect(status().isBadRequest());
    }
}
