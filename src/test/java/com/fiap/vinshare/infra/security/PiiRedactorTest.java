package com.fiap.vinshare.infra.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiRedactorTest {

    private final PiiRedactor redactor = new PiiRedactor();

    @Test
    void removeCpfFormatadoESemFormatacao() {
        assertThat(redactor.redact("cpf 123.456.789-09 ou 12345678909"))
                .isEqualTo("cpf [CPF removido] ou [CPF removido]");
    }

    @Test
    void removeEmail() {
        assertThat(redactor.redact("fale comigo em joao.silva@email.com"))
                .isEqualTo("fale comigo em [e-mail removido]");
    }

    @Test
    void removeTelefone() {
        assertThat(redactor.redact("ligue (11) 98765-4321")).isEqualTo("ligue [telefone removido]");
        assertThat(redactor.redact("ligue +55 11 98765-4321")).isEqualTo("ligue [telefone removido]");
    }

    @Test
    void mantemTextoSemDadosPessoais() {
        String texto = "Quando é minha revisão de 10.000 km?";
        assertThat(redactor.redact(texto)).isEqualTo(texto);
    }

    @Test
    void aceitaNulo() {
        assertThat(redactor.redact(null)).isNull();
    }
}
