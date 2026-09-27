package com.fiap.vinshare;

import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.Dealership;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.domain.entities.Vehicle;
import com.fiap.vinshare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ciclo de vida do agendamento: criação, conflitos, cancelamento, check-in e conclusão. */
class AppointmentFlowTest extends IntegrationTest {

    private static final AtomicInteger SLOT = new AtomicInteger();

    private static String futureSlot() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).plusMinutes(SLOT.incrementAndGet())
                .withNano(0).toString();
    }

    private ResultActions create(String token, UUID vehicleId, UUID dealershipId, String when) throws Exception {
        return mockMvc.perform(post("/appointments")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"vehicleId":"%s","dealershipId":"%s","serviceTypeId":"REVIEW","scheduledAt":"%s"}
                        """.formatted(vehicleId, dealershipId, when)));
    }

    private String createdId(ResultActions actions) throws Exception {
        return json(actions.andReturn()).at("/data/id").asText();
    }

    @Test
    void criarAgendamentoRetorna201ComLocation() throws Exception {
        Dealership d = fixtures.dealership();
        Customer c = fixtures.customer();
        Vehicle v = fixtures.vehicle(c);

        MvcResult result = create(fixtures.tokenFor(c.getUser()), v.getId(), d.getId(), futureSlot())
                .andExpect(status().isCreated())
                .andReturn();

        String id = json(result).at("/data/id").asText();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/appointments/" + id);
    }

    @Test
    void agendarNoPassadoRetorna422() throws Exception {
        Dealership d = fixtures.dealership();
        Customer c = fixtures.customer();
        Vehicle v = fixtures.vehicle(c);
        String past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).withNano(0).toString();

        create(fixtures.tokenFor(c.getUser()), v.getId(), d.getId(), past)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void conflitoDeHorarioRetorna409() throws Exception {
        Dealership d = fixtures.dealership();
        String slot = futureSlot();
        Customer c1 = fixtures.customer();
        Customer c2 = fixtures.customer();
        create(fixtures.tokenFor(c1.getUser()), fixtures.vehicle(c1).getId(), d.getId(), slot)
                .andExpect(status().isCreated());

        create(fixtures.tokenFor(c2.getUser()), fixtures.vehicle(c2).getId(), d.getId(), slot)
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarDuasVezesRetorna409NaSegunda() throws Exception {
        Dealership d = fixtures.dealership();
        Customer c = fixtures.customer();
        String token = fixtures.tokenFor(c.getUser());
        String id = createdId(create(token, fixtures.vehicle(c).getId(), d.getId(), futureSlot()));

        mockMvc.perform(patch("/appointments/" + id + "/cancel").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));
        mockMvc.perform(patch("/appointments/" + id + "/cancel").header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void analistaDeOutraConcessionariaRecebe403NoCheckIn() throws Exception {
        Dealership d1 = fixtures.dealership();
        Dealership d2 = fixtures.dealership();
        User analystD2 = fixtures.analyst(d2);
        Customer c = fixtures.customer();
        String id = createdId(create(fixtures.tokenFor(c.getUser()), fixtures.vehicle(c).getId(), d1.getId(), futureSlot()));

        mockMvc.perform(patch("/appointments/" + id + "/check-in")
                        .header("Authorization", bearer(fixtures.tokenFor(analystD2))))
                .andExpect(status().isForbidden());
    }

    @Test
    void fluxoCompletoCreditaPontosNoCliente() throws Exception {
        Dealership d = fixtures.dealership();
        String analystToken = fixtures.tokenFor(fixtures.analyst(d));
        Customer c = fixtures.customer();
        String clientToken = fixtures.tokenFor(c.getUser());
        String id = createdId(create(clientToken, fixtures.vehicle(c).getId(), d.getId(), futureSlot()));

        mockMvc.perform(patch("/appointments/" + id + "/check-in").header("Authorization", bearer(analystToken)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/appointments/" + id + "/complete")
                        .header("Authorization", bearer(analystToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalAmount\": 350.00, \"summary\": \"Revisão sem pendências\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/me/loyalty/balance").header("Authorization", bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(35));
    }

    @Test
    void concluirComValorAcimaDoLimiteRetorna400() throws Exception {
        Dealership d = fixtures.dealership();
        String analystToken = fixtures.tokenFor(fixtures.analyst(d));
        Customer c = fixtures.customer();
        String id = createdId(create(fixtures.tokenFor(c.getUser()), fixtures.vehicle(c).getId(), d.getId(), futureSlot()));

        mockMvc.perform(patch("/appointments/" + id + "/complete")
                        .header("Authorization", bearer(analystToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalAmount\": 10000000.00}"))
                .andExpect(status().isBadRequest());
    }
}
