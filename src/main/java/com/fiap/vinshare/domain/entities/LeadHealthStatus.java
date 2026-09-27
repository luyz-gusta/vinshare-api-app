package com.fiap.vinshare.domain.entities;

import java.math.BigDecimal;

/**
 * Status de saúde do lead derivado do riskScore. A heurística é a mesma que o
 * app mobile usava client-side; consolidamos no back para deixar a regra
 * num único lugar e habilitar filtro server-side em /leads?status=.
 */
public enum LeadHealthStatus {
    NOVO(20, 60),
    EM_RISCO(60, 80),
    PERDIDO(80, 1000),
    RECUPERADO(0, 20);

    private final BigDecimal minRisk;
    private final BigDecimal maxRiskExclusive;

    LeadHealthStatus(int minRisk, int maxRiskExclusive) {
        this.minRisk = BigDecimal.valueOf(minRisk);
        this.maxRiskExclusive = BigDecimal.valueOf(maxRiskExclusive);
    }

    /** Menor riskScore (inclusivo) que produz este status. */
    public BigDecimal minRisk() {
        return minRisk;
    }

    /** Limite superior (exclusivo) de riskScore para este status. */
    public BigDecimal maxRiskExclusive() {
        return maxRiskExclusive;
    }

    public static LeadHealthStatus fromRiskScore(BigDecimal score) {
        if (score == null) return NOVO;
        double s = score.doubleValue();
        if (s >= 80) return PERDIDO;
        if (s >= 60) return EM_RISCO;
        if (s < 20) return RECUPERADO;
        return NOVO;
    }
}
