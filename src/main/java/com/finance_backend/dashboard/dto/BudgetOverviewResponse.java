package com.finance_backend.dashboard.dto;

import com.finance_backend.budget.dto.BudgetResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** Shows the total of all budgets together with the details of each budget. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetOverviewResponse {

    private BigDecimal totalBudgeted;
    private BigDecimal totalSpent;
    private double overallPercentUsed;
    private List<BudgetResponse> budgets;
}