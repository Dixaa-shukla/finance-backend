package com.finance_backend.dashboard.controller;

import com.finance_backend.dashboard.dto.BudgetOverviewResponse;
import com.finance_backend.dashboard.dto.ChartGroupBy;
import com.finance_backend.dashboard.dto.ChartResponse;
import com.finance_backend.dashboard.dto.DashboardSummaryResponse;
import com.finance_backend.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/v1/dashboard/summary/{userId}
     */
    @GetMapping("/summary/{userId}")
    public ResponseEntity<DashboardSummaryResponse> getSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(dashboardService.getSummary(userId));
    }

    /**
     * GET /api/v1/dashboard/expense-chart/{userId}?groupBy=CATEGORY
     * GET /api/v1/dashboard/expense-chart/{userId}?groupBy=MONTH&months=6
     */
    @GetMapping("/expense-chart/{userId}")
    public ResponseEntity<ChartResponse> getExpenseChart(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "CATEGORY") ChartGroupBy groupBy,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(dashboardService.getExpenseChart(userId, groupBy, months));
    }

    /**
     * Income chart, same shape/semantics as the expense chart.
     * GET /api/v1/dashboard/income-chart/{userId}?groupBy=CATEGORY
     * GET /api/v1/dashboard/income-chart/{userId}?groupBy=MONTH&months=6
     */
    @GetMapping("/income-chart/{userId}")
    public ResponseEntity<ChartResponse> getIncomeChart(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "CATEGORY") ChartGroupBy groupBy,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(dashboardService.getIncomeChart(userId, groupBy, months));
    }

    /**
     * All active budgets with spend vs. limit, plus overall totals.
     * GET /api/v1/dashboard/budget-overview/{userId}
     */
    @GetMapping("/budget-overview/{userId}")
    public ResponseEntity<BudgetOverviewResponse> getBudgetOverview(@PathVariable Long userId) {
        return ResponseEntity.ok(dashboardService.getBudgetOverview(userId));
    }

    /**
     * GET /api/v1/dashboard/savings-report/{userId}?months=6
     */
    @GetMapping("/savings-report/{userId}")
    public ResponseEntity<ChartResponse> getSavingsReport(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(dashboardService.getSavingsReport(userId, months));
    }
}
