package com.fiap.vinshare.specs;

import com.fiap.vinshare.domain.dto.appointment.AppointmentResponseDTO;
import com.fiap.vinshare.domain.dto.auth.MeResponseDTO;
import com.fiap.vinshare.domain.dto.device.DeviceResponseDTO;
import com.fiap.vinshare.domain.dto.device.RegisterDeviceRequestDTO;
import com.fiap.vinshare.domain.dto.service.ServiceRecordResponseDTO;
import com.fiap.vinshare.domain.dto.vehicle.VehicleResponseDTO;
import com.fiap.vinshare.domain.entities.AppointmentStatus;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Me", description = "Endpoints do usuário autenticado")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
public interface MeControllerSpecs {

    @Operation(summary = "Dados do usuário autenticado")
    ResponseEntity<ApiSingleResponse<MeResponseDTO>> me();

    @Operation(summary = "Veículos do cliente autenticado")
    ResponseEntity<ApiSingleResponse<List<VehicleResponseDTO>>> myVehicles();

    @Operation(summary = "Histórico de serviços do cliente autenticado, opcionalmente filtrado por veículo")
    ResponseEntity<ApiSingleResponse<Page<ServiceRecordResponseDTO>>> myServices(
            @RequestParam(required = false) UUID vehicleId, Pageable pageable);

    @Operation(summary = "Agendamentos do cliente autenticado")
    ResponseEntity<ApiSingleResponse<Page<AppointmentResponseDTO>>> myAppointments(
            @RequestParam(required = false) AppointmentStatus status, Pageable pageable);

    @Operation(summary = "Registra um token de push (Expo) do dispositivo")
    ResponseEntity<ApiSingleResponse<DeviceResponseDTO>> registerDevice(@Valid RegisterDeviceRequestDTO request);

    @Operation(summary = "Remove (revoga) um token de push do dispositivo")
    ResponseEntity<Void> removeDevice(String token);
}
