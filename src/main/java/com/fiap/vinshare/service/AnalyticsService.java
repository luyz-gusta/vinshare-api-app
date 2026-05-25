package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.analytics.AnalyticsKpisDTO;
import com.fiap.vinshare.domain.dto.analytics.VinShareByDealershipDTO;
import com.fiap.vinshare.domain.dto.analytics.VinShareSeriesPointDTO;
import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import com.fiap.vinshare.repositories.AnalyticsRepository;
import com.fiap.vinshare.repositories.CustomerSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final CustomerSegmentRepository segmentRepository;

    @Transactional(readOnly = true)
    public AnalyticsKpisDTO getKpis(int monthsBack) {
        OffsetDateTime from = OffsetDateTime.now().minusMonths(monthsBack);
        Object raw = analyticsRepository.computeKpis(from);
        Object[] row = raw instanceof Object[] arr && arr.length > 0 && arr[0] instanceof Object[] r
                ? r
                : (Object[]) raw;

        long underWarranty = ((Number) row[0]).longValue();
        BigDecimal vinShare = row[1] == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString());
        BigDecimal revenue = row[2] == null ? BigDecimal.ZERO : new BigDecimal(row[2].toString());

        Map<CustomerSegmentType, Long> distribution = segmentRepository.countLatestBySegment();
        long leadsAtRisk = distribution.getOrDefault(CustomerSegmentType.ESQUECIDO, 0L)
                + distribution.getOrDefault(CustomerSegmentType.ABANDONO, 0L);

        return AnalyticsKpisDTO.builder()
                .vehiclesUnderWarranty(underWarranty)
                .vinSharePercent(vinShare)
                .estimatedRevenue(revenue)
                .leadsAtRisk(leadsAtRisk)
                .build();
    }

    @Transactional(readOnly = true)
    public List<VinShareSeriesPointDTO> getSeries(LocalDate from, LocalDate to) {
        OffsetDateTime effectiveFrom = from == null
                ? OffsetDateTime.now().minusMonths(12)
                : from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime effectiveTo = to == null
                ? OffsetDateTime.now()
                : to.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        Object raw = analyticsRepository.computeKpis(effectiveFrom);
        Object[] kpisRow = raw instanceof Object[] arr && arr.length > 0 && arr[0] instanceof Object[] r
                ? r
                : (Object[]) raw;
        long under = kpisRow == null || kpisRow[0] == null ? 0L : ((Number) kpisRow[0]).longValue();

        return analyticsRepository.seriesByMonth(effectiveFrom, effectiveTo).stream()
                .map(row -> {
                    LocalDate month = toFirstOfMonth(row[0]);
                    long served = ((Number) row[1]).longValue();
                    BigDecimal share = under == 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(served)
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(under), 1, RoundingMode.HALF_UP);
                    return VinShareSeriesPointDTO.builder()
                            .month(month)
                            .vehiclesWithService(served)
                            .vinSharePercent(share)
                            .build();
                })
                .toList();
    }

    /**
     * O Postgres retorna o resultado de date_trunc sobre timestamptz como
     * OffsetDateTime via JDBC moderno (também pode vir Timestamp em alguns
     * drivers e Instant em outros). Lida com todos os casos para evitar
     * ClassCastException.
     */
    private static LocalDate toFirstOfMonth(Object bucket) {
        if (bucket == null) return LocalDate.now().withDayOfMonth(1);
        if (bucket instanceof OffsetDateTime odt) return odt.toLocalDate().withDayOfMonth(1);
        if (bucket instanceof java.time.Instant inst) {
            return inst.atOffset(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
        }
        if (bucket instanceof Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate().withDayOfMonth(1);
        }
        if (bucket instanceof java.sql.Date d) return d.toLocalDate().withDayOfMonth(1);
        // fallback: tenta parsear o toString como ISO
        return LocalDate.parse(bucket.toString().substring(0, 10)).withDayOfMonth(1);
    }

    @Transactional(readOnly = true)
    public List<VinShareByDealershipDTO> shareByDealership(int monthsBack) {
        OffsetDateTime from = OffsetDateTime.now().minusMonths(monthsBack);
        OffsetDateTime prevFrom = OffsetDateTime.now().minusMonths(monthsBack * 2L);
        return analyticsRepository.shareByDealership(from, prevFrom).stream()
                .map(row -> {
                    UUID id = (UUID) row[0];
                    String name = (String) row[1];
                    long served = ((Number) row[2]).longValue();
                    long total = ((Number) row[3]).longValue();
                    BigDecimal revenue = row[4] == null
                            ? BigDecimal.ZERO
                            : new BigDecimal(row[4].toString()).setScale(2, RoundingMode.HALF_UP);
                    long servedPrev = ((Number) row[5]).longValue();

                    BigDecimal share = total == 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(served)
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);

                    VinShareByDealershipDTO.Trend trend;
                    if (served > servedPrev) trend = VinShareByDealershipDTO.Trend.UP;
                    else if (served < servedPrev) trend = VinShareByDealershipDTO.Trend.DOWN;
                    else trend = VinShareByDealershipDTO.Trend.FLAT;

                    return VinShareByDealershipDTO.builder()
                            .dealershipId(id)
                            .name(name)
                            .vehiclesServed(served)
                            .vehiclesTotal(total)
                            .sharePercent(share)
                            .estimatedRevenue(revenue)
                            .trend(trend)
                            .build();
                })
                .toList();
    }

    /** Helper para parser de período em mês. */
    public static LocalDate firstOfMonth() {
        return OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
    }
}
