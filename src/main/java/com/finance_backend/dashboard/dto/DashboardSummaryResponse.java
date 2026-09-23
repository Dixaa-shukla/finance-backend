package com.finance_backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Home-screen summary -- one call, everything a dashboard landing page needs. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private Long userId;

    private BigDecimal totalIncomeThisMonth;
    private BigDecimal totalExpenseThisMonth;
    private BigDecimal netSavingsThisMonth;

    private int healthScore;
    private String healthScoreLabel;

    private int activeBudgetsCount;
    private int triggeredBudgetAlertsCount;

    private int activeGoalsCount;
    private int goalsNearingDeadlineOrExpiredCount;

    private BigDecimal totalInvestmentValue;
    private BigDecimal totalInvestmentGainLoss;

    private long unreadNotificationsCount;
}
