package com.fiap.vinshare;

import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.Dealership;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.support.IntegrationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Todo erro sai como application/problem+json, inclusive os gerados nos filtros de segurança. */
class ErrorHandlingTest extends IntegrationTest {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private String clientToken() {
        return fixtures.tokenFor(fixtures.customer().getUser());
    }

    @Test
    void jsonMalformadoRetorna400ProblemDetail() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"))
                .andExpect(jsonPath("$.type").value("https://api.fordvinshare.fiap/errors/400"));
    }

    @Test
    void enumInvalidoNoCorpoRetorna400() throws Exception {
        mockMvc.perform(post("/me/devices")
                        .header("Authorization", bearer(clientToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"ExponentPushToken[teste]\",\"platform\":\"WINDOWS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void parametroObrigatorioAusenteRetorna400() throws Exception {
        mockMvc.perform(get("/dealerships").header("Authorization", bearer(clientToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("lat")));
    }

    @Test
    void rotaInexistenteRetorna404ProblemDetail() throws Exception {
        mockMvc.perform(get("/rota-que-nao-existe").header("Authorization", bearer(clientToken())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void metodoNaoSuportadoRetorna405() throws Exception {
        mockMvc.perform(delete("/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void semTokenRetorna401ProblemDetailComDesafioBearer() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    void tokenMalformadoRetorna401InvalidToken() throws Exception {
        mockMvc.perform(get("/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("error=\"invalid_token\"")));
    }

    @Test
    void tokenExpiradoRetorna401ComDetalheDeExpiracao() throws Exception {
        User user = fixtures.customer().getUser();
        Instant past = Instant.now().minus(Duration.ofHours(1));
        String expired = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("ford-vinshare-api")
                .audience().add("vinshare-clients").and()
                .issuedAt(Date.from(past.minus(Duration.ofMinutes(15))))
                .expiration(Date.from(past))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/me").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", containsString("expirado")));
    }

    @Test
    void acaoDeLeadSemTemplateIdNaoGera500() throws Exception {
        Dealership dealership = fixtures.dealership();
        User analyst = fixtures.analyst(dealership);
        Customer customer = fixtures.customer(dealership);

        mockMvc.perform(post("/leads/" + customer.getId() + "/actions")
                        .header("Authorization", bearer(fixtures.tokenFor(analyst)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"WHATSAPP\"}"))
                .andExpect(status().is2xxSuccessful());
    }
}
