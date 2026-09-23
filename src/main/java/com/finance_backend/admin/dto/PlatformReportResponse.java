package com.finance_backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformReportResponse {

    private LocalDateTime generatedAt;
    private PlatformAnalyticsResponse analytics;
    private AiUsageSummaryResponse aiUsage;
}
