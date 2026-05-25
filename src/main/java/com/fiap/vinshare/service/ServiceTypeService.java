package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.dealership.ServiceTypeResponseDTO;
import com.fiap.vinshare.repositories.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;

    @Transactional(readOnly = true)
    public List<ServiceTypeResponseDTO> listAll() {
        return serviceTypeRepository.findAll().stream()
                .map(s -> ServiceTypeResponseDTO.builder()
                        .id(s.getId())
                        .label(s.getLabel())
                        .freeWithWarranty(s.isFreeWithWarranty())
                        .build())
                .toList();
    }
}
