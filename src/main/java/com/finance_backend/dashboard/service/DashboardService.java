package com.finance_backend.dashboard.service;

import com.finance_backend.dashboard.dto.BudgetOverviewResponse;
import com.finance_backend.dashboard.dto.ChartGroupBy;
import com.finance_backend.dashboard.dto.ChartResponse;
import com.finance_backend.dashboard.dto.DashboardSummaryResponse;

public interface DashboardService {

    DashboardSummaryResponse getSummary(Long userId);

    /** groupBy=CATEGORY -> current month's spend per category. groupBy=MONTH -> total spend per month for the last `months` months. */
    ChartResponse getExpenseChart(Long userId, ChartGroupBy groupBy, int months);

    /** Same shape/semantics as getExpenseChart, for Income. */
    ChartResponse getIncomeChart(Long userId, ChartGroupBy groupBy, int months);

    BudgetOverviewResponse getBudgetOverview(Long userId);

    /** Income vs Expense vs Savings, one line per dataset, over the last `months` months. */
    ChartResponse getSavingsReport(Long userId, int months);
}
