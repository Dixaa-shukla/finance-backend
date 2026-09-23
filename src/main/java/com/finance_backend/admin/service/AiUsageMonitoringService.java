package com.finance_backend.admin.service;

import com.finance_backend.admin.dto.AiUsageLogResponse;
import com.finance_backend.admin.dto.AiUsageSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AiUsageMonitoringService {

    /**
     * AI adoption (derived from the feature tables) plus call telemetry
     * (from {@code ai_usage_log}) in one payload.
     */
    AiUsageSummaryResponse getAiUsage();

    /**
     * Individual provider-call rows, newest first --
     */
    Page<AiUsageLogResponse> getAiUsageLogs(Pageable pageable);
}
