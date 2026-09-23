package com.finance_backend.ai.spendingAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

/** One row of SpendingTrendResponse.months -- a single month's spend breakdown. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySpendingBreakdown {

    /** e.g. "2026-08" */
    private String month;
    private BigDecimal totalAmount;
    private Map<String, BigDecimal> byCategory;
}
