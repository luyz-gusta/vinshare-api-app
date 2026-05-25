package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.appointment.AppointmentResponseDTO;
import com.fiap.vinshare.domain.dto.appointment.CompleteAppointmentRequestDTO;
import com.fiap.vinshare.domain.dto.appointment.CreateAppointmentRequestDTO;
import com.fiap.vinshare.domain.dto.appointment.PartUsedDTO;
import com.fiap.vinshare.domain.entities.Analyst;
import com.fiap.vinshare.domain.entities.Appointment;
import com.fiap.vinshare.domain.entities.AppointmentStatus;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.Dealership;
import com.fiap.vinshare.domain.entities.PartUsed;
import com.fiap.vinshare.domain.entities.ServiceRecord;
import com.fiap.vinshare.domain.entities.ServiceType;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.domain.entities.Vehicle;
import com.fiap.vinshare.infra.errors.exceptions.BusinessRuleException;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.infra.security.InputSanitizer;
import com.fiap.vinshare.repositories.AnalystRepository;
import com.fiap.vinshare.repositories.AppointmentRepository;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.DealershipRepository;
import com.fiap.vinshare.repositories.PartUsedRepository;
import com.fiap.vinshare.repositories.ServiceRecordRepository;
import com.fiap.vinshare.repositories.ServiceTypeRepository;
import com.fiap.vinshare.repositories.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CHECKED_IN);

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final DealershipRepository dealershipRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final PartUsedRepository partUsedRepository;
    private final AnalystRepository analystRepository;
    private final LoyaltyService loyaltyService;
    private final InputSanitizer sanitizer;

    @Transactional
    public AppointmentResponseDTO create(CreateAppointmentRequestDTO req, User user) {
        Customer customer = requireCustomer(user);

        Vehicle vehicle = vehicleRepository.findByIdAndCustomerId(req.vehicleId(), customer.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Veículo", req.vehicleId()));

        Dealership dealership = dealershipRepository.findById(req.dealershipId())
                .orElseThrow(() -> ResourceNotFoundException.of("Concessionária", req.dealershipId()));

        ServiceType serviceType = serviceTypeRepository.findById(req.serviceTypeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Tipo de serviço", req.serviceTypeId()));

        if (req.scheduledAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessRuleException("Não é possível agendar em uma data passada");
        }
        if (appointmentRepository.existsByDealershipIdAndScheduledAtAndStatusIn(
                dealership.getId(), req.scheduledAt(), ACTIVE_STATUSES)) {
            throw new BusinessRuleException("Já existe um agendamento neste horário para essa concessionária");
        }

        Appointment appointment = Appointment.builder()
                .customer(customer)
                .vehicle(vehicle)
                .dealership(dealership)
                .serviceType(serviceType)
                .scheduledAt(req.scheduledAt())
                .status(AppointmentStatus.SCHEDULED)
                .notes(sanitizer.sanitize(req.notes()))
                .build();
        appointment = appointmentRepository.save(appointment);
        log.info("Agendamento criado: id={}, cliente={}", appointment.getId(), customer.getId());
        return toDTO(appointment);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponseDTO> listMine(User user, AppointmentStatus status, Pageable pageable) {
        Customer customer = requireCustomer(user);
        Page<Appointment> page = (status == null)
                ? appointmentRepository.findAllByCustomerIdOrderByScheduledAtDesc(customer.getId(), pageable)
                : appointmentRepository.findAllByCustomerIdAndStatusInOrderByScheduledAtDesc(
                customer.getId(), List.of(status), pageable);
        return page.map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public AppointmentResponseDTO getOwned(UUID id, User user) {
        Customer customer = requireCustomer(user);
        return appointmentRepository.findByIdAndCustomerId(id, customer.getId())
                .map(this::toDTO)
                .orElseThrow(() -> ResourceNotFoundException.of("Agendamento", id));
    }

    @Transactional
    public AppointmentResponseDTO cancel(UUID id, User user) {
        Customer customer = requireCustomer(user);
        Appointment appointment = appointmentRepository.findByIdAndCustomerId(id, customer.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Agendamento", id));
        if (!appointment.isOpen()) {
            throw new BusinessRuleException("Apenas agendamentos abertos podem ser cancelados");
        }
        appointment.setStatus(AppointmentStatus.CANCELED);
        return toDTO(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponseDTO checkIn(UUID id, User analystUser) {
        Analyst analyst = requireAnalyst(analystUser);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Agendamento", id));
        ensureSameDealership(analyst, appointment.getDealership());
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleException("Somente agendamentos SCHEDULED podem fazer check-in");
        }
        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        return toDTO(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponseDTO complete(UUID id, CompleteAppointmentRequestDTO req, User analystUser) {
        Analyst analyst = requireAnalyst(analystUser);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Agendamento", id));
        ensureSameDealership(analyst, appointment.getDealership());

        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN
                && appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleException("Agendamento não está em estado finalizável");
        }
        if (serviceRecordRepository.findByAppointmentId(id).isPresent()) {
            throw new BusinessRuleException("Serviço já registrado para esse agendamento");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        ServiceRecord record = ServiceRecord.builder()
                .appointment(appointment)
                .vehicle(appointment.getVehicle())
                .dealership(appointment.getDealership())
                .serviceType(appointment.getServiceType())
                .performedAt(OffsetDateTime.now())
                .totalAmount(req.totalAmount())
                .summary(sanitizer.sanitize(req.summary()))
                .build();
        record = serviceRecordRepository.save(record);

        if (req.partsUsed() != null) {
            ServiceRecord finalRecord = record;
            List<PartUsed> parts = req.partsUsed().stream()
                    .map((PartUsedDTO p) -> PartUsed.builder()
                            .service(finalRecord)
                            .partName(sanitizer.sanitize(p.partName()))
                            .quantity(p.quantity())
                            .unitPrice(p.unitPrice())
                            .build())
                    .toList();
            partUsedRepository.saveAll(parts);
        }

        // Ganho de pontos: 1 ponto a cada R$ 10 (regra simples para o demo)
        int pontos = req.totalAmount().intValue() / 10;
        if (pontos > 0) {
            loyaltyService.earnFromService(appointment.getCustomer(), record, pontos);
        }

        log.info("Agendamento concluído: id={}, serviço={}", appointment.getId(), record.getId());
        return toDTO(appointment);
    }

    private Customer requireCustomer(User user) {
        return customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o usuário logado"));
    }

    private Analyst requireAnalyst(User user) {
        return analystRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Analista não encontrado para o usuário logado"));
    }

    private void ensureSameDealership(Analyst analyst, Dealership dealership) {
        if (!analyst.getDealership().getId().equals(dealership.getId())) {
            throw new BusinessRuleException("Analista não pertence à concessionária do agendamento");
        }
    }

    private AppointmentResponseDTO toDTO(Appointment a) {
        return AppointmentResponseDTO.builder()
                .id(a.getId())
                .vehicleId(a.getVehicle().getId())
                .vehicleModel(a.getVehicle().getModel())
                .dealershipId(a.getDealership().getId())
                .dealershipName(a.getDealership().getName())
                .serviceTypeId(a.getServiceType().getId())
                .serviceTypeLabel(a.getServiceType().getLabel())
                .scheduledAt(a.getScheduledAt())
                .status(a.getStatus())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
