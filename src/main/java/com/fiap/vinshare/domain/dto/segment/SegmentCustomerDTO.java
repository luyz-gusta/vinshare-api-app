package com.fiap.vinshare.domain.dto.segment;

import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cliente navegável de um segmento. Diferente de {@link CustomerSegmentDTO}
 * (que expõe a predição ML), este DTO carrega os campos que a UI precisa para
 * listar clientes do segmento.
 */
@Builder
public record SegmentCustomerDTO(
        UUID customerId,
        String name,
        String cpfMasked,
        CustomerSegmentType segment,
        BigDecimal riskScore,
        LocalDate lastVisitAt,
        BigDecimal estimatedLtv
) {}
