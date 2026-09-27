package com.fiap.vinshare;

import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.support.IntegrationTest;
import com.fiap.vinshare.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PATCH /me/password: senha atual obrigatória, sem deslogar por erro, com limite e encerrando sessões. */
class ChangePasswordTest extends IntegrationTest {

    private static final String NEW_PASSWORD = "Nova-senha-segura-456";

    private ResultActions change(String token, String current, String next) throws Exception {
        return mockMvc.perform(patch("/me/password")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(current, next)));
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn();
    }

    @Test
    void trocaDeSenhaRetorna204EPassaAValerANova() throws Exception {
        Customer c = fixtures.customer();
        String email = c.getUser().getEmail();

        change(fixtures.tokenFor(c.getUser()), TestFixtures.PASSWORD, NEW_PASSWORD)
                .andExpect(status().isNoContent());

        assertThat(login(email, TestFixtures.PASSWORD).getResponse().getStatus()).isEqualTo(401);
        assertThat(login(email, NEW_PASSWORD).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void senhaAtualIncorretaRetorna422ENaoDerrubaASessao() throws Exception {
        String token = fixtures.tokenFor(fixtures.customer().getUser());

        change(token, "senha-errada-123", NEW_PASSWORD).andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/me").header("Authorization", bearer(token))).andExpect(status().isOk());
    }

    @Test
    void novaSenhaIgualAAtualRetorna422() throws Exception {
        String token = fixtures.tokenFor(fixtures.customer().getUser());
        change(token, TestFixtures.PASSWORD, TestFixtures.PASSWORD).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cincoErrosBloqueiamATrocaDeSenha() throws Exception {
        String token = fixtures.tokenFor(fixtures.customer().getUser());
        for (int i = 0; i < 5; i++) {
            change(token, "senha-errada-123", NEW_PASSWORD).andExpect(status().isUnprocessableEntity());
        }
        change(token, TestFixtures.PASSWORD, NEW_PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void trocaDeSenhaEncerraAsOutrasSessoes() throws Exception {
        Customer c = fixtures.customer();
        String refresh = json(login(c.getUser().getEmail(), TestFixtures.PASSWORD)).at("/data/refreshToken").asText();

        change(fixtures.tokenFor(c.getUser()), TestFixtures.PASSWORD, NEW_PASSWORD)
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
