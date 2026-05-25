package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.segment.CustomerSegmentDTO;
import com.fiap.vinshare.domain.dto.segment.SegmentBucketDTO;
import com.fiap.vinshare.domain.dto.segment.SegmentCustomerDTO;
import com.fiap.vinshare.domain.dto.segment.SegmentDistributionResponseDTO;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.CustomerSegment;
import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import com.fiap.vinshare.domain.entities.ServiceRecord;
import com.fiap.vinshare.domain.entities.Vehicle;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.repositories.CustomerSegmentRepository;
import com.fiap.vinshare.repositories.ServiceRecordRepository;
import com.fiap.vinshare.repositories.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SegmentService {

    private final CustomerSegmentRepository segmentRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public CustomerSegmentDTO getCustomerSegment(UUID customerId) {
        CustomerSegment seg = segmentRepository.findFirstByCustomerIdOrderByPredictedAtDesc(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Segmento do cliente", customerId));
        return toDTO(seg);
    }

    @Transactional(readOnly = true)
    public SegmentDistributionResponseDTO distribution() {
        List<Object[]> rows = segmentRepository.distributionAggregated();
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        List<SegmentBucketDTO> buckets = rows.stream()
                .map(row -> {
                    CustomerSegmentType segment = CustomerSegmentType.valueOf((String) row[0]);
                    long count = ((Number) row[1]).longValue();
                    BigDecimal percent = total == 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
                    BigDecimal avgTicket = row[2] == null
                            ? BigDecimal.ZERO
                            : new BigDecimal(row[2].toString()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal avgNps = row[3] == null
                            ? BigDecimal.ZERO
                            : new BigDecimal(row[3].toString()).setScale(1, RoundingMode.HALF_UP);
                    return SegmentBucketDTO.builder()
                            .segment(segment)
                            .count(count)
                            .percent(percent)
                            .avgTicket(avgTicket)
                            .avgNps(avgNps)
                            .build();
                })
                .toList();

        return SegmentDistributionResponseDTO.builder()
                .totalCustomers(total)
                .buckets(buckets)
                .computedAt(OffsetDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<SegmentCustomerDTO> listBySegment(CustomerSegmentType type, Pageable pageable) {
        Page<SegmentCustomerDTO> page = segmentRepository.findLatestBySegment(type.name(), pageable)
                .map(this::toSegmentCustomer);
        auditService.checkBulkQuery("customer_segments", page.getTotalElements());
        return page;
    }

    private SegmentCustomerDTO toSegmentCustomer(CustomerSegment seg) {
        Customer c = seg.getCustomer();
        List<Vehicle> vehicles = vehicleRepository.findAllByCustomerId(c.getId());

        List<ServiceRecord> services = vehicles.stream()
                .flatMap(v -> serviceRecordRepository
                        .findAllByVehicleIdOrderByPerformedAtDesc(v.getId()).stream())
                .toList();

        LocalDate lastVisit = services.stream()
                .map(s -> s.getPerformedAt().toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(null);

        BigDecimal estimatedLtv = services.stream()
                .map(ServiceRecord::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return SegmentCustomerDTO.builder()
                .customerId(c.getId())
                .name(c.getFullName())
                .cpfMasked(c.getCpfMasked())
                .segment(seg.getSegment())
                .riskScore(seg.getRiskScore())
                .lastVisitAt(lastVisit)
                .estimatedLtv(estimatedLtv)
                .build();
    }

    private CustomerSegmentDTO toDTO(CustomerSegment seg) {
        return CustomerSegmentDTO.builder()
                .customerId(seg.getCustomer().getId())
                .segment(seg.getSegment())
                .riskScore(seg.getRiskScore())
                .topFeatures(seg.getTopFeatures())
                .modelVersion(seg.getModelVersion())
                .predictedAt(seg.getPredictedAt())
                .build();
    }
}
