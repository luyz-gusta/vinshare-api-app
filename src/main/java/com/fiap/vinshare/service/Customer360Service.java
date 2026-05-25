package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.customer.Customer360DTO;
import com.fiap.vinshare.domain.dto.customer.TimelineEventDTO;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.CustomerSegment;
import com.fiap.vinshare.domain.entities.NpsResponse;
import com.fiap.vinshare.domain.entities.ServiceRecord;
import com.fiap.vinshare.domain.entities.Vehicle;
import com.fiap.vinshare.domain.entities.Warranty;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.infra.security.CryptoService;
import com.fiap.vinshare.repositories.AppointmentRepository;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.CustomerSegmentRepository;
import com.fiap.vinshare.repositories.NpsResponseRepository;
import com.fiap.vinshare.repositories.ServiceRecordRepository;
import com.fiap.vinshare.repositories.VehicleRepository;
import com.fiap.vinshare.repositories.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class Customer360Service {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final WarrantyRepository warrantyRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final NpsResponseRepository npsRepository;
    private final AppointmentRepository appointmentRepository;
    private final CustomerSegmentRepository segmentRepository;
    private final CryptoService cryptoService;

    @Transactional(readOnly = true)
    public Customer360DTO get360(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", customerId));

        Vehicle vehicle = vehicleRepository.findAllByCustomerId(customer.getId()).stream()
                .findFirst().orElse(null);
        Warranty warranty = vehicle == null ? null : warrantyRepository.findByVehicleId(vehicle.getId()).orElse(null);

        List<ServiceRecord> services = vehicle == null ? List.of()
                : serviceRecordRepository.findAllByVehicleIdOrderByPerformedAtDesc(vehicle.getId());
        BigDecimal totalSpent = services.stream()
                .map(ServiceRecord::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgNps = services.stream()
                .map(s -> npsRepository.findByServiceId(s.getId())
                        .map(NpsResponse::getScore)
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long npsCount = services.stream()
                .filter(s -> npsRepository.existsByServiceId(s.getId()))
                .count();
        avgNps = npsCount == 0 ? BigDecimal.ZERO
                : avgNps.divide(BigDecimal.valueOf(npsCount), 1, RoundingMode.HALF_UP);

        CustomerSegment seg = segmentRepository
                .findFirstByCustomerIdOrderByPredictedAtDesc(customer.getId()).orElse(null);

        String cpfPlain = safeDecrypt(customer.getCpfEncrypted());
        Customer360DTO.CustomerInfo customerInfo = Customer360DTO.CustomerInfo.builder()
                .id(customer.getId())
                .name(customer.getFullName())
                .cpfMasked(cryptoService.mask(cpfPlain))
                .phone(customer.getPhone())
                .build();

        Customer360DTO.VehicleInfo vehicleInfo = vehicle == null ? null
                : Customer360DTO.VehicleInfo.builder()
                        .id(vehicle.getId())
                        .model(vehicle.getModel())
                        .year(vehicle.getYear() == null ? null : vehicle.getYear().intValue())
                        .currentKm(vehicle.getCurrentKm())
                        .warrantyStatus(warranty == null ? null : warranty.getStatus())
                        .build();

        Customer360DTO.SegmentInfo segmentInfo = seg == null ? null
                : Customer360DTO.SegmentInfo.builder()
                        .name(seg.getSegment())
                        .riskScore(seg.getRiskScore())
                        .build();

        return Customer360DTO.builder()
                .customer(customerInfo)
                .vehicle(vehicleInfo)
                .segment(segmentInfo)
                .lifetimeStats(Customer360DTO.LifetimeStats.builder()
                        .servicesCount(services.size())
                        .totalSpent(totalSpent)
                        .averageNps(avgNps)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TimelineEventDTO> timeline(UUID customerId, OffsetDateTime from, OffsetDateTime to) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", customerId));

        List<TimelineEventDTO> events = new ArrayList<>();
        List<Vehicle> vehicles = vehicleRepository.findAllByCustomerId(customer.getId());

        for (Vehicle v : vehicles) {
            for (ServiceRecord s : serviceRecordRepository.findAllByVehicleIdOrderByPerformedAtDesc(v.getId())) {
                if (withinRange(s.getPerformedAt(), from, to)) {
                    events.add(TimelineEventDTO.builder()
                            .at(s.getPerformedAt())
                            .type("SERVICE")
                            .title("Serviço realizado: " + s.getServiceType().getLabel())
                            .description(s.getDealership().getName())
                            .build());
                }
                npsRepository.findByServiceId(s.getId()).ifPresent(n -> {
                    if (withinRange(n.getCreatedAt(), from, to)) {
                        events.add(TimelineEventDTO.builder()
                                .at(n.getCreatedAt())
                                .type("NPS")
                                .title("Avaliação NPS")
                                .description("Nota " + n.getScore())
                                .build());
                    }
                });
            }
        }

        appointmentRepository.findAllByCustomerIdOrderByScheduledAtDesc(
                        customer.getId(), PageRequest.of(0, 100))
                .forEach(ap -> {
                    if (withinRange(ap.getCreatedAt(), from, to)) {
                        events.add(TimelineEventDTO.builder()
                                .at(ap.getCreatedAt())
                                .type("APPOINTMENT")
                                .title("Agendamento " + ap.getStatus())
                                .description(ap.getDealership().getName())
                                .build());
                    }
                });

        events.sort(Comparator.comparing(TimelineEventDTO::at).reversed());
        return events;
    }

    private boolean withinRange(OffsetDateTime moment, OffsetDateTime from, OffsetDateTime to) {
        if (moment == null) return false;
        if (from != null && moment.isBefore(from)) return false;
        if (to != null && moment.isAfter(to)) return false;
        return true;
    }

    private String safeDecrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return "";
        try {
            return cryptoService.decrypt(encrypted);
        } catch (Exception ex) {
            return "";
        }
    }
}
