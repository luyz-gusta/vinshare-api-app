package com.fiap.vinshare.infra.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cifra CPF em repouso (AES-GCM) e gera um HMAC-SHA256 determinístico
 * para a coluna `cpf_lookup_hash`, permitindo busca exata sem expor o valor.
 *
 * Cobre Cybersecurity frente 4 (Dados e Privacidade).
 */
@Slf4j
@Service
public class CryptoService {

    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN = 12;

    @Value("${security.encryption.cpf-aes-key:}")
    private String aesKeyB64;

    @Value("${security.encryption.cpf-hmac-key:}")
    private String hmacKey;

    private SecretKey aesKey;
    private SecretKey hmacSecret;
    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    void init() {
        if (aesKeyB64 == null || aesKeyB64.isBlank()) {
            log.warn("CPF_AES_KEY não definida; gerando uma chave em memória. Defina em produção!");
            byte[] key = new byte[32];
            random.nextBytes(key);
            aesKey = new SecretKeySpec(key, "AES");
        } else {
            byte[] decoded = Base64.getDecoder().decode(aesKeyB64);
            if (decoded.length != 32) {
                throw new IllegalStateException("CPF_AES_KEY deve ter 32 bytes (256 bits) em Base64");
            }
            aesKey = new SecretKeySpec(decoded, "AES");
        }

        String hmacSeed = (hmacKey == null || hmacKey.isBlank())
                ? "fallback-hmac-key-change-in-production"
                : hmacKey;
        hmacSecret = new SecretKeySpec(hmacSeed.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String encrypt(String plainCpf) {
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainCpf.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar CPF", e);
        }
    }

    public String decrypt(String encrypted) {
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[IV_LEN];
            byte[] cipherText = new byte[combined.length - IV_LEN];
            System.arraycopy(combined, 0, iv, 0, IV_LEN);
            System.arraycopy(combined, IV_LEN, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar CPF", e);
        }
    }

    public String hash(String plainCpf) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacSecret);
            byte[] digest = mac.doFinal(plainCpf.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar HMAC do CPF", e);
        }
    }

    public String mask(String plainCpf) {
        if (plainCpf == null || plainCpf.length() < 11) return "***";
        return "***.***." + plainCpf.substring(6, 9) + "-**";
    }
}
