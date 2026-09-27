package com.fiap.vinshare;

import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.repositories.AuditLogRepository;
import com.fiap.vinshare.support.IntegrationTest;
import com.fiap.vinshare.support.TestData;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Eventos de segurança viram métricas (alertas/dashboards) e trilha de auditoria. */
@AutoConfigureObservability(tracing = false)
class SecurityEventsTest extends IntegrationTest {

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private double count(String name, String... tags) {
        Counter counter = registry.find(name).tags(tags).counter();
        return counter == null ? 0 : counter.count();
    }

    @Test
    void falhaDeLoginIncrementaMetrica() throws Exception {
        double before = count("vinshare.security.login", "outcome", "failure");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao-existe@teste.com\",\"password\":\"senha-errada-123\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(count("vinshare.security.login", "outcome", "failure")).isEqualTo(before + 1);
    }

    @Test
    void requisicaoSemTokenIncrementaRejeitados401() throws Exception {
        double before = count("vinshare.security.rejected", "status", "401");
        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
        assertThat(count("vinshare.security.rejected", "status", "401")).isEqualTo(before + 1);
    }

    @Test
    void acessoNegadoIncrementaRejeitados403() throws Exception {
        double before = count("vinshare.security.rejected", "status", "403");
        String client = fixtures.tokenFor(fixtures.customer().getUser());
        mockMvc.perform(get("/analytics/kpis").header("Authorization", bearer(client)))
                .andExpect(status().isForbidden());
        assertThat(count("vinshare.security.rejected", "status", "403")).isEqualTo(before + 1);
    }

    @Test
    void falhaDeLoginFicaAuditadaMesmoRespondendo401() throws Exception {
        String email = TestData.uniqueEmail("falha");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"senha-errada-123\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
        assertThat(auditLogRepository.findAll())
                .anySatisfy(a -> {
                    assertThat(a.getAction()).isEqualTo("LOGIN_FAILURE");
                    assertThat(a.getPayload()).containsEntry("email", email);
                });
    }

    @Test
    void erroDeSenhaAtualNaTrocaDeSenhaGeraLoginSuspeitoNaQuintaVez() throws Exception {
        Customer c = fixtures.customer();
        String token = fixtures.tokenFor(c.getUser());
        String body = "{\"currentPassword\":\"senha-errada-123\",\"newPassword\":\"Outra-senha-forte-1\"}";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(patch("/me/password").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnprocessableEntity());
        }
        assertThat(auditLogRepository.findAll())
                .anySatisfy(a -> {
                    assertThat(a.getAction()).isEqualTo("SUSPICIOUS_LOGIN");
                    assertThat(a.getPayload()).containsEntry("email", c.getUser().getEmail());
                });
    }

    @Test
    void acessoATimelineDoClienteFicaAuditado() throws Exception {
        Customer c = fixtures.customer(fixtures.dealership());
        mockMvc.perform(get("/customers/" + c.getId() + "/timeline")
                        .header("Authorization", bearer(fixtures.tokenFor(fixtures.admin()))))
                .andExpect(status().isOk());
        assertThat(auditLogRepository.findAll())
                .anySatisfy(a -> {
                    assertThat(a.getAction()).isEqualTo("PII_ACCESS");
                    assertThat(a.getResourceId()).isEqualTo(c.getId());
                    assertThat(a.getPayload()).containsEntry("view", "timeline");
                });
    }

    @Test
    void acessoAoCliente360FicaAuditado() throws Exception {
        Customer c = fixtures.customer(fixtures.dealership());
        mockMvc.perform(get("/customers/" + c.getId() + "/360")
                        .header("Authorization", bearer(fixtures.tokenFor(fixtures.admin()))))
                .andExpect(status().isOk());
        assertThat(auditLogRepository.findAll())
                .anySatisfy(a -> {
                    assertThat(a.getAction()).isEqualTo("PII_ACCESS");
                    assertThat(a.getResourceId()).isEqualTo(c.getId());
                });
    }
}
