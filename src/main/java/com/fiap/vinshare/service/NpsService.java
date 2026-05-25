package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.analytics.NpsSummaryDTO;
import com.fiap.vinshare.domain.dto.nps.CreateNpsRequestDTO;
import com.fiap.vinshare.domain.dto.nps.NpsResponseDTO;
import com.fiap.vinshare.domain.dto.nps.PendingSurveyDTO;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.NpsResponse;
import com.fiap.vinshare.domain.entities.ServiceRecord;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.infra.errors.exceptions.BusinessRuleException;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.infra.security.InputSanitizer;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.NpsResponseRepository;
import com.fiap.vinshare.repositories.ServiceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NpsService {

    private final NpsResponseRepository npsRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final CustomerRepository customerRepository;
    private final InputSanitizer sanitizer;

    @Transactional(readOnly = true)
    public List<PendingSurveyDTO> listPending(User user) {
        Customer customer = requireCustomer(user);
        List<UUID> serviceIds = npsRepository.findPendingServiceIdsForCustomer(customer.getId());
        return serviceRecordRepository.findAllById(serviceIds).stream()
                .map(s -> PendingSurveyDTO.builder()
                        .serviceId(s.getId())
                        .dealershipName(s.getDealership().getName())
                        .serviceTypeLabel(s.getServiceType().getLabel())
                        .performedAt(s.getPerformedAt())
                        .build())
                .toList();
    }

    @Transactional
    public NpsResponseDTO submit(UUID serviceId, CreateNpsRequestDTO req, User user) {
        Customer customer = requireCustomer(user);
        ServiceRecord service = serviceRecordRepository.findById(serviceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Serviço", serviceId));
        if (!service.getVehicle().getCustomer().getId().equals(customer.getId())) {
            throw ResourceNotFoundException.of("Serviço", serviceId);
        }
        if (npsRepository.existsByServiceId(serviceId)) {
            throw new BusinessRuleException("NPS já enviado para esse serviço");
        }
        NpsResponse response = NpsResponse.builder()
                .service(service)
                .customer(customer)
                .score(req.score())
                .comment(sanitizer.sanitize(req.comment()))
                .likedCategories(req.likedCategories())
                .improvementCategories(req.improvementCategories())
                .build();
        response = npsRepository.save(response);
        return toDTO(response);
    }

    @Transactional(readOnly = true)
    public NpsSummaryDTO summary(int monthsBack) {
        OffsetDateTime from = OffsetDateTime.now().minusMonths(monthsBack);
        Object raw = npsRepository.summary(from);
        Object[] row = raw instanceof Object[] arr && arr.length > 0 && arr[0] instanceof Object[] r
                ? r
                : (Object[]) raw;

        long total = ((Number) row[0]).longValue();
        BigDecimal avg = row[1] == null
                ? BigDecimal.ZERO
                : new BigDecimal(row[1].toString()).setScale(2, RoundingMode.HALF_UP);
        long promoters = ((Number) row[2]).longValue();
        long passives = ((Number) row[3]).longValue();
        long detractors = ((Number) row[4]).longValue();

        BigDecimal nps = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(promoters - detractors)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);

        return NpsSummaryDTO.builder()
                .totalResponses(total)
                .averageScore(avg)
                .npsScore(nps)
                .promoters(promoters)
                .passives(passives)
                .detractors(detractors)
                .computedAt(OffsetDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public NpsResponseDTO getByServiceId(UUID serviceId, User user) {
        Customer customer = requireCustomer(user);
        NpsResponse response = npsRepository.findByServiceId(serviceId)
                .orElseThrow(() -> ResourceNotFoundException.of("NPS do serviço", serviceId));
        if (!response.getCustomer().getId().equals(customer.getId())) {
            throw ResourceNotFoundException.of("NPS do serviço", serviceId);
        }
        return toDTO(response);
    }

    private Customer requireCustomer(User user) {
        return customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
    }

    private NpsResponseDTO toDTO(NpsResponse r) {
        return NpsResponseDTO.builder()
                .id(r.getId())
                .serviceId(r.getService().getId())
                .score(r.getScore())
                .comment(r.getComment())
                .likedCategories(r.getLikedCategories())
                .improvementCategories(r.getImprovementCategories())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
