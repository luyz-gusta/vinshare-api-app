package com.fiap.vinshare.infra.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testes unitários do CryptoService (criptografia de CPF em repouso, LGPD/Cyber). */
class CryptoServiceTest {

    private static final String AES_KEY_B64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String HMAC_KEY = "chave-hmac-de-teste-nao-usar-em-producao";

    private CryptoService crypto;

    private static CryptoService build(String aesKey, String hmacKey) {
        CryptoService service = new CryptoService();
        ReflectionTestUtils.setField(service, "aesKeyB64", aesKey);
        ReflectionTestUtils.setField(service, "hmacKey", hmacKey);
        ReflectionTestUtils.invokeMethod(service, "init");
        return service;
    }

    @BeforeEach
    void setUp() {
        crypto = build(AES_KEY_B64, HMAC_KEY);
    }

    @Test
    @DisplayName("encrypt/decrypt faz roundtrip do CPF original")
    void encryptDecryptRoundtrip() {
        String encrypted = crypto.encrypt("12345678901");
        assertThat(encrypted).isNotBlank().isNotEqualTo("12345678901");
        assertThat(crypto.decrypt(encrypted)).isEqualTo("12345678901");
    }

    @Test
    @DisplayName("encrypt usa IV aleatório, gerando textos cifrados distintos")
    void encryptIsNonDeterministic() {
        assertThat(crypto.encrypt("12345678901")).isNotEqualTo(crypto.encrypt("12345678901"));
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

    @Test
    @DisplayName("sem CPF_AES_KEY a aplicação não sobe (nunca gera chave aleatória)")
    void initFalhaSemChaveAes() {
        assertThatThrownBy(() -> build("", HMAC_KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CPF_AES_KEY");
    }

    @Test
    @DisplayName("CPF_HMAC_KEY curta é rejeitada (nunca usa chave padrão)")
    void initFalhaComChaveHmacCurta() {
        assertThatThrownBy(() -> build(AES_KEY_B64, "curta"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CPF_HMAC_KEY");
    }
}
