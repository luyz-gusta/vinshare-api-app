package com.fiap.vinshare.domain.entities;

import java.math.BigDecimal;

/**
 * Status de saúde do lead derivado do riskScore. A heurística é a mesma que o
 * app mobile usava client-side; consolidamos no back para deixar a regra
 * num único lugar e habilitar filtro server-side em /leads?status=.
 */
public enum LeadHealthStatus {
    NOVO,
    EM_RISCO,
    PERDIDO,
    RECUPERADO;

    public static LeadHealthStatus fromRiskScore(BigDecimal score) {
        if (score == null) return NOVO;
        double s = score.doubleValue();
        if (s >= 80) return PERDIDO;
        if (s >= 60) return EM_RISCO;
        if (s < 20) return RECUPERADO;
        return NOVO;
    }
}
