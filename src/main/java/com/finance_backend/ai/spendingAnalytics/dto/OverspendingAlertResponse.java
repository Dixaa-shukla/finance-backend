package com.finance_backend.ai.spendingAnalytics.dto;

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
public class OverspendingAlertResponse {

    private String category;
    private BigDecimal currentMonthAmount;
    /** Average monthly spend for this category over the lookback window. */
    private BigDecimal historicalAverageAmount;
    private double percentAboveAverage;
}