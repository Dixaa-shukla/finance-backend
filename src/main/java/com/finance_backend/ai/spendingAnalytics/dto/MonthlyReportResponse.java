package com.finance_backend.ai.spendingAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyReportResponse {

    /** Added for Module 14 (Notifications) -- lets a notification deep-link to this specific report. */
    private Long id;
    private Long userId;
    /** e.g. "2026-07" */
    private String month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal savingsAmount;
    private double savingsRatePercent;
    private int healthScore;
    private String healthScoreLabel;
    private String aiInsight;
    private LocalDateTime generatedAt;
}
