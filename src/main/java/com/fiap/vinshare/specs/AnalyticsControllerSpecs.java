package com.fiap.vinshare.specs;

import com.fiap.vinshare.domain.dto.analytics.AnalyticsKpisDTO;
import com.fiap.vinshare.domain.dto.analytics.NpsSummaryDTO;
import com.fiap.vinshare.domain.dto.analytics.VinShareByDealershipDTO;
import com.fiap.vinshare.domain.dto.analytics.VinShareSeriesPointDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.specs.error.ApiResponseForbidden;
import com.fiap.vinshare.specs.error.ApiResponseInternalServerError;
import com.fiap.vinshare.specs.error.ApiResponseUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Analytics", description = "KPIs e VIN Share da rede (apenas analista/admin)")
@ApiResponseInternalServerError
@ApiResponseUnauthorized
@ApiResponseForbidden
public interface AnalyticsControllerSpecs {

    @Operation(summary = "KPIs principais do dashboard")
    ResponseEntity<ApiSingleResponse<AnalyticsKpisDTO>> kpis(@RequestParam(required = false) Integer monthsBack);

    @Operation(summary = "Série temporal mensal do VIN Share. Datas em ISO date (YYYY-MM-DD), opcionais.")
    ResponseEntity<ApiSingleResponse<List<VinShareSeriesPointDTO>>> series(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @Operation(summary = "VIN Share por concessionária")
    ResponseEntity<ApiSingleResponse<List<VinShareByDealershipDTO>>> byDealership(
            @RequestParam(required = false) Integer monthsBack);

    @Operation(summary = "Resumo de NPS para o dashboard analítico")
    ResponseEntity<ApiSingleResponse<NpsSummaryDTO>> nps(@RequestParam(required = false) Integer monthsBack);
}
