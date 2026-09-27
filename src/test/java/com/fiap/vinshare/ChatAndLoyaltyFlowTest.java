package com.fiap.vinshare;

import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.Reward;
import com.fiap.vinshare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatAndLoyaltyFlowTest extends IntegrationTest {

    @Test
    void abrirSessaoRetorna201ComLocationEMensagemRetorna201() throws Exception {
        String token = fixtures.tokenFor(fixtures.customer().getUser());

        MvcResult session = mockMvc.perform(post("/chat/sessions").header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = json(session).at("/data/sessionId").asText();
        assertThat(session.getResponse().getHeader("Location")).endsWith("/chat/sessions/" + sessionId + "/messages");

        mockMvc.perform(post("/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Quando é minha próxima revisão?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("ASSISTANT"));
    }

    @Test
    void resgateRetorna201ENovoSaldo() throws Exception {
        Customer c = fixtures.customer();
        Reward reward = fixtures.reward(100);
        fixtures.setBalance(c, 500);

        mockMvc.perform(post("/me/loyalty/redeem")
                        .header("Authorization", bearer(fixtures.tokenFor(c.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rewardId\":\"" + reward.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newBalance").value(400));
    }

    @Test
    void mensagemComDadosPessoaisNaoChegaAoProvedorDeIa() throws Exception {
        String token = fixtures.tokenFor(fixtures.customer().getUser());
        String sessionId = json(mockMvc.perform(post("/chat/sessions").header("Authorization", bearer(token)))
                .andExpect(status().isCreated()).andReturn()).at("/data/sessionId").asText();

        MvcResult result = mockMvc.perform(post("/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Meu CPF é 123.456.789-09 e meu e-mail é joao.silva@email.com\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String reply = json(result).at("/data/content").asText();
        assertThat(reply)
                .doesNotContain("123.456.789-09")
                .doesNotContain("joao.silva@email.com")
                .contains("[CPF removido]");
    }
}
