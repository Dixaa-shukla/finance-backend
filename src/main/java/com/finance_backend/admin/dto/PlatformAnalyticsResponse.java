package com.finance_backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformAnalyticsResponse {

    private PlatformOverviewResponse overview;

    private BigDecimal platformTotalSpent;
    private BigDecimal platformTotalEarned;
    /** earned - spent across the whole platform. */
    private BigDecimal platformNetFlow;
}
