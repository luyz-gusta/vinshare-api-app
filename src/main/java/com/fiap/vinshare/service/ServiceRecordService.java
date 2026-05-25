package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.service.ServiceRecordResponseDTO;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.ServiceRecord;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.ServiceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRecordService {

    private final ServiceRecordRepository serviceRecordRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Page<ServiceRecordResponseDTO> listMine(User user, UUID vehicleId, Pageable pageable) {
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        Page<ServiceRecord> page = vehicleId == null
                ? serviceRecordRepository
                        .findAllByVehicle_CustomerIdOrderByPerformedAtDesc(customer.getId(), pageable)
                : serviceRecordRepository
                        .findAllByVehicle_CustomerIdAndVehicle_IdOrderByPerformedAtDesc(
                                customer.getId(), vehicleId, pageable);
        return page.map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public ServiceRecordResponseDTO findOwnedById(UUID id, User user) {
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        ServiceRecord record = serviceRecordRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Serviço", id));
        if (!record.getVehicle().getCustomer().getId().equals(customer.getId())) {
            throw ResourceNotFoundException.of("Serviço", id);
        }
        return toDTO(record);
    }

    private ServiceRecordResponseDTO toDTO(ServiceRecord r) {
        return ServiceRecordResponseDTO.builder()
                .id(r.getId())
                .vehicleId(r.getVehicle().getId())
                .vehicleModel(r.getVehicle().getModel())
                .dealershipId(r.getDealership().getId())
                .dealershipName(r.getDealership().getName())
                .serviceTypeId(r.getServiceType().getId())
                .serviceTypeLabel(r.getServiceType().getLabel())
                .performedAt(r.getPerformedAt())
                .totalAmount(r.getTotalAmount())
                .summary(r.getSummary())
                .build();
    }
}
