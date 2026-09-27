package com.fiap.vinshare;

import com.fiap.vinshare.domain.entities.Reward;
import com.fiap.vinshare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cada perfil tem responsabilidades próprias: ADMIN gerencia catálogo, ANALYST opera a concessionária. */
class ProfilesTest extends IntegrationTest {

    private String adminToken() {
        return fixtures.tokenFor(fixtures.admin());
    }

    private String clientToken() {
        return fixtures.tokenFor(fixtures.customer().getUser());
    }

    private static String rewardBody(String name, int cost) {
        return "{\"name\":\"%s\",\"description\":\"Desconto especial\",\"pointsCost\":%d}".formatted(name, cost);
    }

    @Test
    void adminNaoFazCheckIn() throws Exception {
        mockMvc.perform(patch("/appointments/" + UUID.randomUUID() + "/check-in")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNaoDisparaAcaoDeLead() throws Exception {
        mockMvc.perform(post("/leads/" + UUID.randomUUID() + "/actions")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"WHATSAPP\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCriaPremioCom201ELocation() throws Exception {
        MvcResult result = mockMvc.perform(post("/loyalty/rewards")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rewardBody("Lavagem grátis", 300)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = json(result).at("/data/id").asText();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/loyalty/rewards/" + id);

        mockMvc.perform(get("/loyalty/rewards/" + id).header("Authorization", bearer(clientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointsCost").value(300))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void analistaNaoCriaPremio() throws Exception {
        String analyst = fixtures.tokenFor(fixtures.analyst(fixtures.dealership()));
        mockMvc.perform(post("/loyalty/rewards")
                        .header("Authorization", bearer(analyst))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rewardBody("Indevido", 100)))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteNaoCriaPremio() throws Exception {
        mockMvc.perform(post("/loyalty/rewards")
                        .header("Authorization", bearer(clientToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rewardBody("Indevido", 100)))
                .andExpect(status().isForbidden());
    }

    @Test
    void premioComCustoInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/loyalty/rewards")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rewardBody("Zero", 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminAtualizaPremio() throws Exception {
        Reward reward = fixtures.reward(100);
        mockMvc.perform(put("/loyalty/rewards/" + reward.getId())
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rewardBody("Atualizado", 999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointsCost").value(999));
    }

    @Test
    void adminDesativaPremioECatalogoNaoMostraMais() throws Exception {
        Reward reward = fixtures.reward(100);
        mockMvc.perform(delete("/loyalty/rewards/" + reward.getId()).header("Authorization", bearer(adminToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/loyalty/rewards").header("Authorization", bearer(clientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '%s')]".formatted(reward.getId())).isEmpty());
    }

    @Test
    void premioInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/loyalty/rewards/" + UUID.randomUUID()).header("Authorization", bearer(clientToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void clienteNaoAcessaMetricas() throws Exception {
        mockMvc.perform(get("/actuator/metrics").header("Authorization", bearer(clientToken())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void adminAcessaMetricas() throws Exception {
        mockMvc.perform(get("/actuator/metrics").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
    }
}
