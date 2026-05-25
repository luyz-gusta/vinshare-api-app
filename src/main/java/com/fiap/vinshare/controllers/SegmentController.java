package com.fiap.vinshare.controllers;

import com.fiap.vinshare.domain.dto.segment.CustomerSegmentDTO;
import com.fiap.vinshare.domain.dto.segment.SegmentCustomerDTO;
import com.fiap.vinshare.domain.dto.segment.SegmentDistributionResponseDTO;
import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import com.fiap.vinshare.infra.responses.details.ApiSingleResponse;
import com.fiap.vinshare.service.SegmentService;
import com.fiap.vinshare.specs.SegmentControllerSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
public class SegmentController implements SegmentControllerSpecs {

    private final SegmentService segmentService;

    @Override
    @GetMapping("/segments/distribution")
    public ResponseEntity<ApiSingleResponse<SegmentDistributionResponseDTO>> distribution() {
        return ResponseEntity.ok(ApiSingleResponse.of(segmentService.distribution()));
    }

    @Override
    @GetMapping("/segments/{segment}/customers")
    public ResponseEntity<ApiSingleResponse<Page<SegmentCustomerDTO>>> bySegment(
            @PathVariable CustomerSegmentType segment, Pageable pageable) {
        return ResponseEntity.ok(ApiSingleResponse.of(segmentService.listBySegment(segment, pageable)));
    }

    @Override
    @GetMapping("/customers/{customerId}/segment")
    public ResponseEntity<ApiSingleResponse<CustomerSegmentDTO>> getCustomerSegment(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ApiSingleResponse.of(segmentService.getCustomerSegment(customerId)));
    }
}
