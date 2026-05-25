package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.vehicle.MaintenanceAlertResponseDTO;
import com.fiap.vinshare.domain.dto.vehicle.UpdateOdometerRequestDTO;
import com.fiap.vinshare.domain.dto.vehicle.VehicleResponseDTO;
import com.fiap.vinshare.domain.dto.vehicle.WarrantyResponseDTO;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.domain.entities.Vehicle;
import com.fiap.vinshare.domain.entities.Warranty;
import com.fiap.vinshare.domain.entities.WarrantyStatus;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.MaintenanceAlertRepository;
import com.fiap.vinshare.repositories.VehicleRepository;
import com.fiap.vinshare.repositories.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final WarrantyRepository warrantyRepository;
    private final MaintenanceAlertRepository alertRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<VehicleResponseDTO> listOwnedBy(User user) {
        Customer customer = requireCustomer(user);
        return vehicleRepository.findAllByCustomerId(customer.getId()).stream()
                .map(v -> toDTO(v, warrantyRepository.findByVehicleId(v.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public WarrantyResponseDTO getWarranty(UUID vehicleId, User user) {
        Vehicle v = requireOwnedVehicle(vehicleId, user);
        Warranty w = warrantyRepository.findByVehicleId(v.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Garantia não encontrada"));
        long days = w.daysRemaining();
        boolean freeRev = w.getStatus() == WarrantyStatus.ACTIVE && days > 0;
        return WarrantyResponseDTO.builder()
                .vehicleId(v.getId())
                .status(w.getStatus())
                .startDate(w.getStartDate())
                .endDate(w.getEndDate())
                .daysRemaining(days)
                .freeRevisionAvailable(freeRev)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceAlertResponseDTO> getAlerts(UUID vehicleId, User user) {
        Vehicle v = requireOwnedVehicle(vehicleId, user);
        return alertRepository.findAllByVehicleIdAndDismissedAtIsNullOrderByDueDateAsc(v.getId()).stream()
                .map(a -> MaintenanceAlertResponseDTO.builder()
                        .id(a.getId())
                        .type(a.getType())
                        .title(titleFor(a.getType()))
                        .kmThreshold(a.getKmThreshold())
                        .currentKm(v.getCurrentKm())
                        .kmRemaining(a.getKmThreshold() == null ? null : a.getKmThreshold() - v.getCurrentKm())
                        .dueDate(a.getDueDate())
                        .build())
                .toList();
    }

    @Transactional
    public VehicleResponseDTO updateOdometer(UUID vehicleId, UpdateOdometerRequestDTO req, User user) {
        Vehicle v = requireOwnedVehicle(vehicleId, user);
        v.setCurrentKm(req.km());
        vehicleRepository.save(v);
        return toDTO(v, warrantyRepository.findByVehicleId(v.getId()).orElse(null));
    }

    private Vehicle requireOwnedVehicle(UUID vehicleId, User user) {
        Customer customer = requireCustomer(user);
        return vehicleRepository.findByIdAndCustomerId(vehicleId, customer.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Veículo", vehicleId));
    }

    private Customer requireCustomer(User user) {
        return customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o usuário logado"));
    }

    private VehicleResponseDTO toDTO(Vehicle v, Warranty w) {
        return VehicleResponseDTO.builder()
                .id(v.getId())
                .model(v.getModel())
                .version(v.getVersion())
                .year(v.getYear() == null ? null : v.getYear().intValue())
                .plate(v.getPlate())
                .vin(v.getVin())
                .currentKm(v.getCurrentKm())
                .warrantyStatus(w == null ? null : w.getStatus())
                .build();
    }

    private String titleFor(com.fiap.vinshare.domain.entities.MaintenanceAlertType type) {
        return switch (type) {
            case OIL_CHANGE -> "Troca de óleo recomendada";
            case REVIEW -> "Revisão recomendada";
            case BRAKE -> "Verificar sistema de freios";
            case TIRE -> "Verificar pneus";
            case OTHER -> "Manutenção recomendada";
        };
    }

    @SuppressWarnings("unused")
    private LocalDate referenceToday() { return LocalDate.now(); }
}
