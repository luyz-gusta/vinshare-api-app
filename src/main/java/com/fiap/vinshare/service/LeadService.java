package com.fiap.vinshare.service;

import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import com.fiap.vinshare.domain.dto.lead.LeadActionRequestDTO;
import com.fiap.vinshare.domain.dto.lead.LeadActionResponseDTO;
import com.fiap.vinshare.domain.dto.lead.LeadResponseDTO;
import com.fiap.vinshare.domain.entities.Analyst;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.CustomerSegment;
import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import com.fiap.vinshare.domain.entities.LeadAction;
import com.fiap.vinshare.domain.entities.LeadHealthStatus;
import com.fiap.vinshare.domain.entities.LeadStatus;
import com.fiap.vinshare.domain.entities.ServiceRecord;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.domain.entities.Vehicle;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.infra.security.InputSanitizer;
import com.fiap.vinshare.repositories.AnalystRepository;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.CustomerSegmentRepository;
import com.fiap.vinshare.repositories.CustomerSegmentRepository.LeadSegmentRow;
import com.fiap.vinshare.repositories.LeadActionRepository;
import com.fiap.vinshare.repositories.NpsResponseRepository;
import com.fiap.vinshare.repositories.ServiceRecordRepository;
import com.fiap.vinshare.repositories.VehicleRepository;
import com.fiap.vinshare.repositories.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private static final List<CustomerSegmentType> AT_RISK =
            List.of(CustomerSegmentType.ESQUECIDO, CustomerSegmentType.ABANDONO);

    private final CustomerSegmentRepository segmentRepository;
    private final LeadActionRepository leadActionRepository;
    private final AnalystRepository analystRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final WarrantyRepository warrantyRepository;
    private final NpsResponseRepository npsResponseRepository;
    private final InputSanitizer sanitizer;
    private final AuditService auditService;
    private final CustomerAccessPolicy accessPolicy;

    private static final BigDecimal NO_MAX_RISK = BigDecimal.valueOf(1000);

    @Transactional(readOnly = true)
    public Page<LeadResponseDTO> listLeads(CustomerSegmentType segmentFilter,
                                          LeadHealthStatus statusFilter,
                                          Pageable pageable,
                                          User user) {
        List<String> segments = (segmentFilter != null ? List.of(segmentFilter) : AT_RISK).stream()
                .map(Enum::name)
                .toList();
        BigDecimal minRisk = statusFilter == null ? BigDecimal.ZERO : statusFilter.minRisk();
        BigDecimal maxRisk = statusFilter == null ? NO_MAX_RISK : statusFilter.maxRiskExclusive();
        CustomerAccessPolicy.Scope scope = accessPolicy.scopeFor(user);
        // Ordenação fixa na consulta (maior risco primeiro); ignora o sort enviado pelo cliente.
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return segmentRepository.findLatestScoped(segments, minRisk, maxRisk,
                        scope.allDealerships(), scope.dealershipId(), page)
                .map(this::toLead);
    }

    @Transactional(readOnly = true)
    public LeadResponseDTO findById(UUID customerId, User user) {
        CustomerSegment seg = segmentRepository.findFirstByCustomerIdOrderByPredictedAtDesc(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Lead", customerId));
        accessPolicy.checkAccess(user, seg.getCustomer());
        return toLead(seg);
    }

    @Transactional
    public LeadActionResponseDTO triggerAction(UUID customerId, LeadActionRequestDTO req, User analystUser) {
        Analyst analyst = analystRepository.findByUser(analystUser)
                .orElseThrow(() -> new ResourceNotFoundException("Analista não encontrado"));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", customerId));
        accessPolicy.checkAccess(analystUser, customer);

        Map<String, Object> payload = new HashMap<>();
        payload.put("notes", sanitizer.sanitize(req.notes()));
        payload.put("simulated", true);

        LeadAction action = LeadAction.builder()
                .customer(customer)
                .analyst(analyst)
                .channel(req.channel())
                .templateId(req.templateId())
                .status(LeadStatus.IN_PROGRESS)
                .payload(payload)
                .build();
        action = leadActionRepository.save(action);

        log.info("Ação de lead criada (simulada): cliente={}, canal={}, analista={}",
                customer.getId(), req.channel(), analyst.getId());
        auditService.leadAction(customer.getId(), req.channel().name(), req.templateId());

        return LeadActionResponseDTO.builder()
                .id(action.getId())
                .customerId(customer.getId())
                .analystId(analyst.getId())
                .channel(action.getChannel())
                .templateId(action.getTemplateId())
                .status(action.getStatus())
                .simulated(true)
                .createdAt(action.getCreatedAt())
                .build();
    }

    private LeadResponseDTO toLead(CustomerSegment seg) {
        Customer customer = seg.getCustomer();
        return toLead(customer, seg.getSegment(), seg.getRiskScore(), seg.getPredictedAt());
    }

    private LeadResponseDTO toLead(LeadSegmentRow row) {
        Customer customer = customerRepository.findById(row.getCustomerId())
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", row.getCustomerId()));
        CustomerSegmentType segmentType = CustomerSegmentType.valueOf(row.getSegment());
        java.time.OffsetDateTime predictedAt = row.getPredictedAt().atOffset(java.time.ZoneOffset.UTC);
        return toLead(customer, segmentType, row.getRiskScore(), predictedAt);
    }

    private LeadResponseDTO toLead(Customer customer, CustomerSegmentType segment,
                                    BigDecimal riskScore, java.time.OffsetDateTime predictedAt) {
        Vehicle firstVehicle = vehicleRepository.findAllByCustomerId(customer.getId()).stream()
                .findFirst().orElse(null);

        String vehicleModel = null;
        String vehiclePlate = null;
        Optional<ServiceRecord> lastService = Optional.empty();
        String warrantyStatus = null;
        if (firstVehicle != null) {
            vehicleModel = firstVehicle.getModel() + " "
                    + (firstVehicle.getYear() == null ? "" : firstVehicle.getYear());
            vehiclePlate = firstVehicle.getPlate();
            lastService = serviceRecordRepository
                    .findAllByVehicleIdOrderByPerformedAtDesc(firstVehicle.getId())
                    .stream().findFirst();
            warrantyStatus = warrantyRepository.findByVehicleId(firstVehicle.getId())
                    .map(w -> w.getStatus().name()).orElse(null);
        }

        LocalDate lastVisit = lastService.map(r -> r.getPerformedAt().toLocalDate()).orElse(null);
        Integer daysSinceLastVisit = lastVisit == null
                ? null
                : (int) ChronoUnit.DAYS.between(lastVisit, LocalDate.now());

        Short lastNpsScore = lastService
                .flatMap(s -> npsResponseRepository.findByServiceId(s.getId()))
                .map(n -> n.getScore())
                .orElse(null);

        return LeadResponseDTO.builder()
                .id(customer.getId())
                .customerId(customer.getId())
                .customerName(customer.getFullName())
                .cpfMasked(customer.getCpfMasked())
                .vehicleModel(vehicleModel)
                .vehiclePlate(vehiclePlate)
                .lastVisitAt(lastVisit)
                .daysSinceLastVisit(daysSinceLastVisit)
                .segment(segment)
                .status(LeadHealthStatus.fromRiskScore(riskScore))
                .riskScore(riskScore)
                .warrantyStatus(warrantyStatus)
                .lastNpsScore(lastNpsScore)
                .reason(suggestReason(segment))
                .suggestedAction(suggestAction(segment))
                .updatedAt(predictedAt)
                .build();
    }

    private String suggestReason(CustomerSegmentType segment) {
        return switch (segment) {
            case ABANDONO -> "Cliente fora da rede há mais de 12 meses";
            case ESQUECIDO -> "Cliente atrasou janela de revisão";
            case ECONOMICO -> "Cliente sensível a preço";
            case FIEL -> "Cliente fiel, manter engajado";
        };
    }

    private String suggestAction(CustomerSegmentType type) {
        return switch (type) {
            case ABANDONO -> "Ofertar revisão com 20% de desconto e contato direto";
            case ESQUECIDO -> "Lembrete proativo de revisão";
            case ECONOMICO -> "Comunicar promoções e pacotes";
            case FIEL -> "Programa de fidelidade reforçado";
        };
    }
}
