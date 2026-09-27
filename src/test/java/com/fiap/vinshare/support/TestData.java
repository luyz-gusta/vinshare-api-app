package com.fiap.vinshare.support;

import java.security.SecureRandom;
import java.util.UUID;

/** Geradores de dados únicos para os testes (evitam colisão entre testes no mesmo banco). */
public final class TestData {

    private static final SecureRandom RND = new SecureRandom();

    private TestData() {}

    public static String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
    }

    /** CPF de 11 dígitos com dígitos verificadores válidos. */
    public static String randomCpf() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) d[i] = RND.nextInt(10);
        if (allEqual(d)) d[0] = (d[0] + 1) % 10;
        d[9] = checkDigit(d, 9);
        d[10] = checkDigit(d, 10);
        StringBuilder sb = new StringBuilder();
        for (int x : d) sb.append(x);
        return sb.toString();
    }

    public static String randomPlate() {
        return "TST" + String.format("%04d", RND.nextInt(10_000));
    }

    public static String randomVin() {
        return ("9BF" + UUID.randomUUID().toString().replace("-", "")).substring(0, 17).toUpperCase();
    }

    private static int checkDigit(int[] d, int len) {
        int sum = 0;
        for (int i = 0; i < len; i++) sum += d[i] * ((len + 1) - i);
        int r = (sum * 10) % 11;
        return r == 10 ? 0 : r;
    }

    private static boolean allEqual(int[] d) {
        for (int i = 1; i < 9; i++) if (d[i] != d[0]) return false;
        return true;
    }
}
