package com.fiap.vinshare;

import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.repositories.AuditLogRepository;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.support.IntegrationTest;
import com.fiap.vinshare.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** LGPD art. 18: acesso/portabilidade e eliminação (anonimização) dos dados do titular. */
class DataSubjectRightsTest extends IntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void exportaOsDadosDoProprioTitular() throws Exception {
        Customer c = fixtures.customer();
        fixtures.vehicle(c);
        mockMvc.perform(get("/me/data-export").header("Authorization", bearer(fixtures.tokenFor(c.getUser()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account.email").value(c.getUser().getEmail()))
                .andExpect(jsonPath("$.data.profile.cpf", matchesPattern("\\d{11}")))
                .andExpect(jsonPath("$.data.vehicles.length()").value(1))
                .andExpect(jsonPath("$.data.sharedWith").isNotEmpty());
    }

    @Test
    void exportacaoFicaAuditada() throws Exception {
        Customer c = fixtures.customer();
        mockMvc.perform(get("/me/data-export").header("Authorization", bearer(fixtures.tokenFor(c.getUser()))))
                .andExpect(status().isOk());
        assertThat(auditLogRepository.findAll())
                .anySatisfy(a -> {
                    assertThat(a.getAction()).isEqualTo("DATA_EXPORTED");
                    assertThat(a.getResourceId()).isEqualTo(c.getId());
                });
    }

    @Test
    void analistaNaoUsaExportDeCliente() throws Exception {
        String analyst = fixtures.tokenFor(fixtures.analyst(fixtures.dealership()));
        mockMvc.perform(get("/me/data-export").header("Authorization", bearer(analyst)))
                .andExpect(status().isForbidden());
    }

    private static String currentPassword(String password) {
        return "{\"currentPassword\":\"%s\"}".formatted(password);
    }

    @Test
    void excluirContaSemSenhaAtualRetorna400() throws Exception {
        Customer c = fixtures.customer();
        mockMvc.perform(delete("/me").header("Authorization", bearer(fixtures.tokenFor(c.getUser()))))
                .andExpect(status().isBadRequest());
        assertThat(customerRepository.findById(c.getId()).orElseThrow().getFullName()).isEqualTo(c.getFullName());
    }

    @Test
    void excluirContaComSenhaErradaRetorna422EMantemAConta() throws Exception {
        Customer c = fixtures.customer();
        mockMvc.perform(delete("/me").header("Authorization", bearer(fixtures.tokenFor(c.getUser())))
                        .contentType(MediaType.APPLICATION_JSON).content(currentPassword("senha-errada-123")))
                .andExpect(status().isUnprocessableEntity());
        assertThat(customerRepository.findById(c.getId()).orElseThrow().getFullName()).isEqualTo(c.getFullName());
    }

    @Test
    void cincoSenhasErradasBloqueiamAExclusao() throws Exception {
        Customer c = fixtures.customer();
        String token = fixtures.tokenFor(c.getUser());
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(delete("/me").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content(currentPassword("senha-errada-123")))
                    .andExpect(status().isUnprocessableEntity());
        }
        // Com o token roubado, nem a senha certa passa durante a janela de bloqueio.
        mockMvc.perform(delete("/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(currentPassword(TestFixtures.PASSWORD)))
                .andExpect(status().isTooManyRequests());
        assertThat(customerRepository.findById(c.getId()).orElseThrow().getFullName()).isEqualTo(c.getFullName());
    }

    @Test
    void excluirContaAnonimizaEImpedeAcesso() throws Exception {
        Customer c = fixtures.customer();
        String email = c.getUser().getEmail();
        String token = fixtures.tokenFor(c.getUser());

        mockMvc.perform(delete("/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(currentPassword(TestFixtures.PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, TestFixtures.PASSWORD)))
                .andExpect(status().isUnauthorized());

        Customer anonymized = customerRepository.findById(c.getId()).orElseThrow();
        assertThat(anonymized.getFullName()).isEqualTo("Titular anonimizado");
        assertThat(anonymized.getPhone()).isNull();
    }
}
