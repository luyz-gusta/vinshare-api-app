package com.fiap.vinshare.infra.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Remove dados pessoais de texto livre antes de enviá-lo a provedores
 * externos (IA). LGPD: minimização e transferência internacional.
 * O CPF é tratado antes do telefone porque 11 dígitos casam com os dois.
 */
@Component
public class PiiRedactor {

    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(\\+?55\\s?)?\\(?\\d{2}\\)?\\s?9?\\d{4}[-\\s]?\\d{4}\\b");

    public String redact(String text) {
        if (text == null) return null;
        String out = CPF.matcher(text).replaceAll("[CPF removido]");
        out = EMAIL.matcher(out).replaceAll("[e-mail removido]");
        return PHONE.matcher(out).replaceAll("[telefone removido]");
    }
}
