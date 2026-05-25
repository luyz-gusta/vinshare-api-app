package com.fiap.vinshare.infra.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do CryptoService (criptografia de CPF em repouso, LGPD/Cyber).
 * Sem contexto Spring: a chave AES é gerada em memória (fallback) e o HMAC usa
 * uma chave fixa de teste.
 */
class CryptoServiceTest {

    private CryptoService crypto;

    @BeforeEach
    void setUp() {
        crypto = new CryptoService();
        ReflectionTestUtils.setField(crypto, "aesKeyB64", "");
        ReflectionTestUtils.setField(crypto, "hmacKey", "chave-de-teste-para-hmac");
        ReflectionTestUtils.invokeMethod(crypto, "init");
    }

    @Test
    @DisplayName("encrypt/decrypt faz roundtrip do CPF original")
    void encryptDecryptRoundtrip() {
        String cpf = "12345678901";
        String encrypted = crypto.encrypt(cpf);

        assertThat(encrypted).isNotBlank().isNotEqualTo(cpf);
        assertThat(crypto.decrypt(encrypted)).isEqualTo(cpf);
    }

    @Test
    @DisplayName("encrypt usa IV aleatório, gerando textos cifrados distintos")
    void encryptIsNonDeterministic() {
        assertThat(crypto.encrypt("12345678901"))
                .isNotEqualTo(crypto.encrypt("12345678901"));
    }

    @Test
    @DisplayName("hash é determinístico para o mesmo CPF e distinto entre CPFs")
    void hashIsDeterministic() {
        assertThat(crypto.hash("12345678901")).isEqualTo(crypto.hash("12345678901"));
        assertThat(crypto.hash("12345678901")).isNotEqualTo(crypto.hash("99999999999"));
    }

    @Test
    @DisplayName("mask oculta os dígitos sensíveis do CPF")
    void maskHidesSensitiveDigits() {
        assertThat(crypto.mask("12345678901")).isEqualTo("***.***.789-**");
        assertThat(crypto.mask(null)).isEqualTo("***");
        assertThat(crypto.mask("123")).isEqualTo("***");
    }
}
