package com.fiap.vinshare;

import com.fiap.vinshare.support.IntegrationTest;
import com.fiap.vinshare.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contexto próprio com limites baixos (login 3/min, refresh 6/min) para exercitar o 429. */
@TestPropertySource(properties = {
        "security.rate-limit.login-capacity=3",
        "security.rate-limit.login-refill-tokens=3",
        "security.rate-limit.refresh-capacity=6",
        "security.rate-limit.refresh-refill-tokens=6",
        "security.client-ip.trusted-proxy-hops=0"
})
class RateLimitTest extends IntegrationTest {

    /** E-mail único por chamada: isola o rate limit por IP do bloqueio por e-mail (LoginAttemptService). */
    private ResultActions loginFrom(String ip, String xff) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .with(req -> {
                    req.setRemoteAddr(ip);
                    if (xff != null) req.addHeader("X-Forwarded-For", xff);
                    return req;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"qualquer-senha\"}".formatted(TestData.uniqueEmail("rl"))));
    }

    @Test
    void quartaTentativaNoMesmoIpRecebe429ProblemDetail() throws Exception {
        for (int i = 0; i < 3; i++) loginFrom("10.1.1.1", null).andExpect(status().isUnauthorized());
        loginFrom("10.1.1.1", null)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void xForwardedForForjadoNaoBurlaOLimite() throws Exception {
        for (int i = 0; i < 3; i++) loginFrom("10.2.2.2", "9.9.9." + i).andExpect(status().isUnauthorized());
        loginFrom("10.2.2.2", "9.9.9.200").andExpect(status().isTooManyRequests());
    }

    private ResultActions refreshFrom(String ip) throws Exception {
        return mockMvc.perform(post("/auth/refresh")
                .with(req -> { req.setRemoteAddr(ip); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"token-inexistente\"}"));
    }

    @Test
    void refreshTemLimiteProprioMaiorQueOLogin() throws Exception {
        // Vários aparelhos atrás do mesmo IP (Wi-Fi da sala) renovam a sessão sem cair no limite do login.
        for (int i = 0; i < 6; i++) refreshFrom("10.5.5.5").andExpect(status().isUnauthorized());
        refreshFrom("10.5.5.5").andExpect(status().isTooManyRequests());
    }

    @Test
    void outroIpTemBaldeProprio() throws Exception {
        for (int i = 0; i < 3; i++) loginFrom("10.3.3.3", null).andExpect(status().isUnauthorized());
        loginFrom("10.4.4.4", null).andExpect(status().isUnauthorized());
    }
}
