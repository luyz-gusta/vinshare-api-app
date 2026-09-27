package com.fiap.vinshare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import com.fiap.vinshare.domain.entities.Dealership;
import com.fiap.vinshare.domain.entities.Vehicle;
import com.fiap.vinshare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** OWASP API1 (BOLA): cada perfil só alcança os objetos do próprio escopo. */
class OwnershipTest extends IntegrationTest {

    private List<String> leadCustomerIds(String token, String query) throws Exception {
        JsonNode content = json(mockMvc.perform(get("/leads" + query).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn()).at("/data/content");
        List<String> ids = new ArrayList<>();
        content.forEach(n -> ids.add(n.get("customerId").asText()));
        return ids;
    }

    @Test
    void analistaVeCliente360DaPropriaConcessionaria() throws Exception {
        Dealership d = fixtures.dealership();
        String analyst = fixtures.tokenFor(fixtures.analyst(d));
        Customer c = fixtures.customer(d);

        mockMvc.perform(get("/customers/" + c.getId() + "/360").header("Authorization", bearer(analyst)))
                .andExpect(status().isOk());
    }

    @Test
    void analistaRecebe404ParaClienteDeOutraConcessionaria() throws Exception {
        Dealership d1 = fixtures.dealership();
        Dealership d2 = fixtures.dealership();
        String analystD1 = fixtures.tokenFor(fixtures.analyst(d1));
        Customer c = fixtures.customer(d2);
        fixtures.segment(c, CustomerSegmentType.ABANDONO, 90);

        for (String path : List.of("/customers/%s/360", "/customers/%s/timeline",
                "/customers/%s/segment", "/leads/%s")) {
            mockMvc.perform(get(path.formatted(c.getId())).header("Authorization", bearer(analystD1)))
                    .andExpect(status().isNotFound());
        }
        mockMvc.perform(post("/leads/" + c.getId() + "/actions")
                        .header("Authorization", bearer(analystD1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"WHATSAPP\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminVeClienteDeQualquerConcessionaria() throws Exception {
        Customer c = fixtures.customer(fixtures.dealership());
        mockMvc.perform(get("/customers/" + c.getId() + "/360")
                        .header("Authorization", bearer(fixtures.tokenFor(fixtures.admin()))))
                .andExpect(status().isOk());
    }

    @Test
    void clienteSemConcessionariaSoApareceParaAdmin() throws Exception {
        String analyst = fixtures.tokenFor(fixtures.analyst(fixtures.dealership()));
        Customer c = fixtures.customer();

        mockMvc.perform(get("/customers/" + c.getId() + "/360").header("Authorization", bearer(analyst)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/customers/" + c.getId() + "/360")
                        .header("Authorization", bearer(fixtures.tokenFor(fixtures.admin()))))
                .andExpect(status().isOk());
    }

    @Test
    void primeiroAgendamentoDefineConcessionariaDeRelacionamento() throws Exception {
        Dealership d = fixtures.dealership();
        String analyst = fixtures.tokenFor(fixtures.analyst(d));
        Customer c = fixtures.customer();
        Vehicle v = fixtures.vehicle(c);

        mockMvc.perform(get("/customers/" + c.getId() + "/360").header("Authorization", bearer(analyst)))
                .andExpect(status().isNotFound());

        String when = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5).withNano(0).toString();
        mockMvc.perform(post("/appointments")
                        .header("Authorization", bearer(fixtures.tokenFor(c.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vehicleId":"%s","dealershipId":"%s","serviceTypeId":"REVIEW","scheduledAt":"%s"}
                                """.formatted(v.getId(), d.getId(), when)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/customers/" + c.getId() + "/360").header("Authorization", bearer(analyst)))
                .andExpect(status().isOk());
    }

    @Test
    void listaDeLeadsDoAnalistaSoTrazClientesDaPropriaConcessionaria() throws Exception {
        Dealership d1 = fixtures.dealership();
        Dealership d2 = fixtures.dealership();
        String analystD1 = fixtures.tokenFor(fixtures.analyst(d1));
        Customer a = fixtures.customer(d1);
        fixtures.segment(a, CustomerSegmentType.ABANDONO, 90);
        Customer b = fixtures.customer(d1);
        fixtures.segment(b, CustomerSegmentType.ESQUECIDO, 65);
        Customer outsider = fixtures.customer(d2);
        fixtures.segment(outsider, CustomerSegmentType.ABANDONO, 95);

        assertThat(leadCustomerIds(analystD1, "?size=50"))
                .containsExactlyInAnyOrder(a.getId().toString(), b.getId().toString());
    }

    @Test
    void paginacaoDeLeadsRespeitaOTamanhoDaPagina() throws Exception {
        Dealership d = fixtures.dealership();
        String analyst = fixtures.tokenFor(fixtures.analyst(d));
        for (int i = 0; i < 3; i++) {
            fixtures.segment(fixtures.customer(d), CustomerSegmentType.ABANDONO, 85 + i);
        }

        JsonNode page = json(mockMvc.perform(get("/leads?size=2&page=0").header("Authorization", bearer(analyst)))
                .andExpect(status().isOk()).andReturn()).get("data");
        assertThat(page.get("content")).hasSize(2);
        assertThat(page.get("totalElements").asLong()).isEqualTo(3);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
    }

    @Test
    void filtroDeStatusDeLeadUsaFaixaDeRisco() throws Exception {
        Dealership d = fixtures.dealership();
        String analyst = fixtures.tokenFor(fixtures.analyst(d));
        Customer perdido = fixtures.customer(d);
        fixtures.segment(perdido, CustomerSegmentType.ABANDONO, 90);
        Customer emRisco = fixtures.customer(d);
        fixtures.segment(emRisco, CustomerSegmentType.ESQUECIDO, 65);

        assertThat(leadCustomerIds(analyst, "?status=PERDIDO")).containsExactly(perdido.getId().toString());
    }

    @Test
    void clienteNaoAcessaVeiculoDeOutroCliente() throws Exception {
        Vehicle v = fixtures.vehicle(fixtures.customer());
        fixtures.warranty(v);
        String intruder = fixtures.tokenFor(fixtures.customer().getUser());

        mockMvc.perform(get("/vehicles/" + v.getId() + "/warranty").header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());
    }

    @Test
    void clienteNaoAcessaAgendamentoDeOutroCliente() throws Exception {
        Dealership d = fixtures.dealership();
        Customer owner = fixtures.customer();
        String when = OffsetDateTime.now(ZoneOffset.UTC).plusDays(6).withNano(0).toString();
        String id = json(mockMvc.perform(post("/appointments")
                        .header("Authorization", bearer(fixtures.tokenFor(owner.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vehicleId":"%s","dealershipId":"%s","serviceTypeId":"REVIEW","scheduledAt":"%s"}
                                """.formatted(fixtures.vehicle(owner).getId(), d.getId(), when)))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asText();

        String intruder = fixtures.tokenFor(fixtures.customer().getUser());
        mockMvc.perform(get("/appointments/" + id).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());
    }
}
