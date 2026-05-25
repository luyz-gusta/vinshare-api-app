package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.analytics.AnalyticsKpisDTO;
import com.fiap.vinshare.domain.dto.analytics.NpsSummaryDTO;
import com.fiap.vinshare.domain.dto.analytics.VinShareByDealershipDTO;
import com.fiap.vinshare.domain.dto.analytics.VinShareSeriesPointDTO;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.service.AnalyticsService;
import com.fiap.vinshare.service.NpsService;
import com.fiap.vinshare.specs.AnalyticsControllerSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
public class AnalyticsController implements AnalyticsControllerSpecs {

    private final AnalyticsService analyticsService;
    private final NpsService npsService;

    @Override
    @GetMapping("/kpis")
    public ResponseEntity<ApiSingleResponse<AnalyticsKpisDTO>> kpis(
            @RequestParam(required = false) Integer monthsBack) {
        int m = monthsBack == null ? 12 : monthsBack;
        return ResponseEntity.ok(ApiSingleResponse.of(analyticsService.getKpis(m)));
    }

    @Override
    @GetMapping("/nps")
    public ResponseEntity<ApiSingleResponse<NpsSummaryDTO>> nps(
            @RequestParam(required = false) Integer monthsBack) {
        int m = monthsBack == null ? 12 : monthsBack;
        return ResponseEntity.ok(ApiSingleResponse.of(npsService.summary(m)));
    }

    @Override
    @GetMapping("/vin-share/series")
    public ResponseEntity<ApiSingleResponse<List<VinShareSeriesPointDTO>>> series(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiSingleResponse.of(analyticsService.getSeries(from, to)));
    }

    @Override
    @GetMapping("/vin-share/by-dealership")
    public ResponseEntity<ApiSingleResponse<List<VinShareByDealershipDTO>>> byDealership(
            @RequestParam(required = false) Integer monthsBack) {
        int m = monthsBack == null ? 12 : monthsBack;
        return ResponseEntity.ok(ApiSingleResponse.of(analyticsService.shareByDealership(m)));
    }
}
