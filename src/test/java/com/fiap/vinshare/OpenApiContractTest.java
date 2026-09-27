package com.fiap.vinshare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fiap.vinshare.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O contrato OpenAPI publicado precisa refletir os status e a segurança reais. */
class OpenApiContractTest extends IntegrationTest {

    private JsonNode apiDocs() throws Exception {
        return json(mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn());
    }

    @Test
    void criacaoDeAgendamentoDocumenta201E422() throws Exception {
        JsonNode responses = apiDocs().at("/paths/~1appointments/post/responses");
        assertThat(responses.has("201")).isTrue();
        assertThat(responses.has("422")).isTrue();
    }

    @Test
    void errosReferenciamSchemaProblemDetail() throws Exception {
        String ref = apiDocs()
                .at("/paths/~1appointments/post/responses/400/content/application~1problem+json/schema/$ref")
                .asText();
        assertThat(ref).endsWith("/ProblemDetail");
    }

    @Test
    void logoutEPublicoERetorna204() throws Exception {
        JsonNode op = apiDocs().at("/paths/~1auth~1logout/post");
        assertThat(op.at("/responses").has("204")).isTrue();
        assertThat(op.get("security")).isNotNull();
        assertThat(op.get("security").isEmpty()).isTrue();
    }

    @Test
    void paginacaoApareceComoParametrosSimples() throws Exception {
        List<String> names = new ArrayList<>();
        apiDocs().at("/paths/~1leads/get/parameters").forEach(p -> names.add(p.get("name").asText()));
        assertThat(names).contains("page", "size");
    }
}
