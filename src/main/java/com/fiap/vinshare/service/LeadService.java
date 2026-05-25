package com.fiap.vinshare.service;

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
import com.fiap.vinshare.repositories.LeadActionRepository;
import com.fiap.vinshare.repositories.NpsResponseRepository;
import com.fiap.vinshare.repositories.ServiceRecordRepository;
import com.fiap.vinshare.repositories.VehicleRepository;
import com.fiap.vinshare.repositories.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    @Transactional(readOnly = true)
    public Page<LeadResponseDTO> listLeads(CustomerSegmentType segmentFilter,
                                          LeadHealthStatus statusFilter,
                                          Pageable pageable) {
        CustomerSegmentType[] targets = (segmentFilter != null)
                ? new CustomerSegmentType[]{segmentFilter}
                : AT_RISK.toArray(new CustomerSegmentType[0]);

        List<LeadResponseDTO> all = new java.util.ArrayList<>();
        for (CustomerSegmentType t : targets) {
            segmentRepository.findLatestBySegment(t.name(), pageable)
                    .map(this::toLead)
                    .forEach(all::add);
        }
        if (statusFilter != null) {
            all = new java.util.ArrayList<>(
                    all.stream().filter(l -> l.status() == statusFilter).toList());
        }
        return new PageImpl<>(all, pageable, all.size());
    }

    @Transactional(readOnly = true)
    public LeadResponseDTO findById(UUID customerId) {
        CustomerSegment seg = segmentRepository.findFirstByCustomerIdOrderByPredictedAtDesc(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Lead", customerId));
        return toLead(seg);
    }

    @Transactional
    public LeadActionResponseDTO triggerAction(UUID customerId, LeadActionRequestDTO req, User analystUser) {
        Analyst analyst = analystRepository.findByUser(analystUser)
                .orElseThrow(() -> new ResourceNotFoundException("Analista não encontrado"));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", customerId));

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
                .segment(seg.getSegment())
                .status(LeadHealthStatus.fromRiskScore(seg.getRiskScore()))
                .riskScore(seg.getRiskScore())
                .warrantyStatus(warrantyStatus)
                .lastNpsScore(lastNpsScore)
                .reason(suggestReason(seg))
                .suggestedAction(suggestAction(seg.getSegment()))
                .updatedAt(seg.getPredictedAt())
                .build();
    }

    private String suggestReason(CustomerSegment seg) {
        return switch (seg.getSegment()) {
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
