package com.fiap.vinshare.specs;

import com.fiap.vinshare.specs.error.ApiResponseTooManyRequests;
import com.fiap.vinshare.specs.error.ApiResponseUnprocessableEntity;
import com.fiap.vinshare.specs.error.ApiResponseBadRequest;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.fiap.vinshare.domain.dto.appointment.AppointmentResponseDTO;
import com.fiap.vinshare.domain.dto.auth.ChangePasswordRequestDTO;
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

    @Operation(summary = "Alterar a própria senha (exige a senha atual e encerra as demais sessões)")
    @ApiResponse(responseCode = "204", description = "Senha alterada; refresh tokens do usuário encerrados")
    @ApiResponseBadRequest
    @ApiResponseUnprocessableEntity
    @ApiResponseTooManyRequests
    ResponseEntity<Void> changePassword(@Valid ChangePasswordRequestDTO request);

    @Operation(summary = "Veículos do cliente autenticado")
    ResponseEntity<ApiSingleResponse<List<VehicleResponseDTO>>> myVehicles();

    @Operation(summary = "Histórico de serviços do cliente autenticado, opcionalmente filtrado por veículo")
    ResponseEntity<ApiSingleResponse<Page<ServiceRecordResponseDTO>>> myServices(
            @RequestParam(required = false) UUID vehicleId, @ParameterObject Pageable pageable);

    @Operation(summary = "Agendamentos do cliente autenticado")
    ResponseEntity<ApiSingleResponse<Page<AppointmentResponseDTO>>> myAppointments(
            @RequestParam(required = false) AppointmentStatus status, @ParameterObject Pageable pageable);

    @Operation(summary = "Registra um token de push (Expo) do dispositivo")
    @ApiResponse(responseCode = "201", description = "Dispositivo registrado")
    ResponseEntity<ApiSingleResponse<DeviceResponseDTO>> registerDevice(@Valid RegisterDeviceRequestDTO request);

    @Operation(summary = "Remove (revoga) um token de push do dispositivo")
    @ApiResponse(responseCode = "204", description = "Dispositivo removido")
    ResponseEntity<Void> removeDevice(String token);
}
