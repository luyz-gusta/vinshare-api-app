package com.fiap.vinshare.domain;

import com.fiap.vinshare.domain.entities.LeadHealthStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** A faixa de risco usada no filtro SQL tem que bater com a regra de fromRiskScore. */
class LeadHealthStatusTest {

    @Test
    void faixaDeRiscoEspelhaFromRiskScore() {
        for (int centesimos = 0; centesimos <= 100_00; centesimos += 25) {
            BigDecimal score = BigDecimal.valueOf(centesimos, 2);
            LeadHealthStatus status = LeadHealthStatus.fromRiskScore(score);
            assertThat(score).isGreaterThanOrEqualTo(status.minRisk());
            assertThat(score).isLessThan(status.maxRiskExclusive());
        }
    }
}
