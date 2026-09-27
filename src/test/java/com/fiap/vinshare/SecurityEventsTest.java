package com.fiap.vinshare;

import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.repositories.AuditLogRepository;
import com.fiap.vinshare.support.IntegrationTest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
