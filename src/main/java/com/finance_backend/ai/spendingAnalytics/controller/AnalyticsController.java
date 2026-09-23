package com.finance_backend.ai.spendingAnalytics.controller;

import com.finance_backend.ai.spendingAnalytics.dto.HealthScoreResponse;
import com.finance_backend.ai.spendingAnalytics.dto.MonthlyReportResponse;
import com.finance_backend.ai.spendingAnalytics.dto.OverspendingAlertResponse;
import com.finance_backend.ai.spendingAnalytics.dto.SpendingTrendResponse;
import com.finance_backend.ai.spendingAnalytics.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/spending-analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Live financial health score (0-100) for right now, with sub-scores
     * for transparency. Not persisted -- recomputed on every call.
     * GET /api/v1/ai/spending-analytics/health-score/{userId}
     */
    @GetMapping("/health-score/{userId}")
    public ResponseEntity<HealthScoreResponse> getHealthScore(@PathVariable Long userId) {
        return ResponseEntity.ok(analyticsService.getHealthScore(userId));
    }

    /**
     * Category-wise spending for the last N months (default 6), oldest first.
     * GET /api/v1/ai/spending-analytics/trends/{userId}?months=3
     */
    @GetMapping("/trends/{userId}")
    public ResponseEntity<SpendingTrendResponse> getSpendingTrends(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "6") int months) {
        return ResponseEntity.ok(analyticsService.getSpendingTrends(userId, months));
    }

    /**
     * Categories where this month's spend is significantly above the
     * user's own historical average.
     * GET /api/v1/ai/spending-analytics/overspending/{userId}
     */
    @GetMapping("/overspending/{userId}")
    public ResponseEntity<List<OverspendingAlertResponse>> getOverspendingAlerts(@PathVariable Long userId) {
        return ResponseEntity.ok(analyticsService.getOverspendingAlerts(userId));
    }

    /**
     * Generate (or regenerate) the AI-narrated monthly report for a given
     * month. Defaults to the previous complete month if not specified.
     * POST /api/v1/ai/spending-analytics/monthly-report/{userId}?month=2026-07
     */
    @PostMapping("/monthly-report/{userId}")
    public ResponseEntity<MonthlyReportResponse> generateMonthlyReport(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth targetMonth = (month != null) ? month : YearMonth.now().minusMonths(1);
        return ResponseEntity.ok(analyticsService.generateMonthlyReport(userId, targetMonth));
    }

    /**
     * Fetch an already-generated monthly report.
     * GET /api/v1/ai/spending-analytics/monthly-report/{userId}?month=2026-07
     */
    @GetMapping("/monthly-report/{userId}")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(analyticsService.getMonthlyReport(userId, month));
    }
}
