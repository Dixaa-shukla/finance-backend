package com.finance_backend.admin.controller;

import com.finance_backend.admin.dto.AiUsageLogResponse;
import com.finance_backend.admin.dto.AiUsageSummaryResponse;
import com.finance_backend.admin.service.AiUsageMonitoringService;
import com.finance_backend.common.dtos.ApiResponse;
import com.finance_backend.common.dtos.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ai-usage")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiUsageController {

    /** caller can't request an unbounded page of log rows. */
    private static final int MAX_PAGE_SIZE = 100;

    private final AiUsageMonitoringService aiUsageMonitoringService;

    public AdminAiUsageController(AiUsageMonitoringService aiUsageMonitoringService) {
        this.aiUsageMonitoringService = aiUsageMonitoringService;
    }

    /**
     * AI feature adoption (chatbot volume, learned merchants, generated
     * reports) plus provider call telemetry (failures, fall-overs, latency).
     * GET /api/v1/admin/ai-usage
     *
     * SECURED (Module 1): covered by the class-level @PreAuthorize("hasRole('ADMIN')").
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AiUsageSummaryResponse>> getAiUsage() {
        return ResponseEntity.ok(ApiResponse.success(aiUsageMonitoringService.getAiUsage()));
    }

    /**
     * Individual AI provider calls, newest first -- the drill-down behind the
     * summary's failure and fall-over counts.
     * GET /api/v1/admin/ai-usage/logs?page=0&size=20

     * Sort is fixed to newest-first by the service; page/size only.

     * SECURED (Module 1): covered by the class-level @PreAuthorize("hasRole('ADMIN')").
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<PageResponse<AiUsageLogResponse>>> getAiUsageLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        Page<AiUsageLogResponse> logs = aiUsageMonitoringService.getAiUsageLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(logs)));
    }
}
