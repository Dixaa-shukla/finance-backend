package com.finance_backend.ai.spendingAnalytics.service;

import com.finance_backend.ai.spendingAnalytics.dto.HealthScoreResponse;
import com.finance_backend.ai.spendingAnalytics.dto.MonthlyReportResponse;
import com.finance_backend.ai.spendingAnalytics.dto.OverspendingAlertResponse;
import com.finance_backend.ai.spendingAnalytics.dto.SpendingTrendResponse;

import java.time.YearMonth;
import java.util.List;

public interface AnalyticsService {

    /** Live, computed-on-demand health score. Not persisted. */
    HealthScoreResponse getHealthScore(Long userId);

    /** Category-wise spend for each of the last `months` months, oldest first. */
    SpendingTrendResponse getSpendingTrends(Long userId, int months);

    /** Categories where this month's spend exceeds the user's own historical average. */
    List<OverspendingAlertResponse> getOverspendingAlerts(Long userId);

    /**
     * Generates (or regenerates) the persisted monthly report for the given
     * month -- computes all numeric data deterministically, then asks the
     * AI for a narrative summary (degrades gracefully to a static sentence
     * if both AI providers fail).
     */
    MonthlyReportResponse generateMonthlyReport(Long userId, YearMonth targetMonth);

    /** Fetches an already-generated report. Throws if none exists for that month. */
    MonthlyReportResponse getMonthlyReport(Long userId, YearMonth targetMonth);
}
