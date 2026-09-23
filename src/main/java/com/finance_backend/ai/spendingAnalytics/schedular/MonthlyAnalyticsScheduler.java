package com.finance_backend.ai.spendingAnalytics.schedular;

import com.finance_backend.ai.spendingAnalytics.dto.MonthlyReportResponse;
import com.finance_backend.ai.spendingAnalytics.service.AnalyticsService;
import com.finance_backend.expense.repository.ExpenseRepository;
import com.finance_backend.notification.entity.NotificationType;
import com.finance_backend.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

/**
 * On the 1st of every month, generates the previous month's report for
 * every user.
 * As of Module 14 (Notifications), a successful report now also creates a
 * MONTHLY_SUMMARY notification pointing the user at their new report.
 */
@Slf4j
@Component
public class MonthlyAnalyticsScheduler {

    private final ExpenseRepository expenseRepository;
    private final AnalyticsService analyticsService;
    private final NotificationService notificationService;

    public MonthlyAnalyticsScheduler(ExpenseRepository expenseRepository,
                                     AnalyticsService analyticsService,
                                     NotificationService notificationService) {
        this.expenseRepository = expenseRepository;
        this.analyticsService = analyticsService;
        this.notificationService = notificationService;
    }

    /** Runs at 02:00 on the 1st of every month, covering the month that just ended. */
    @Scheduled(cron = "0 0 2 1 * *")
    public void generatePreviousMonthReportsForAllUsers() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        List<Long> userIds = expenseRepository.findDistinctUserIds();

        log.info("Starting monthly report generation for {} for {} user(s)", previousMonth, userIds.size());

        int successCount = 0;
        for (Long userId : userIds) {
            try {
                MonthlyReportResponse report = analyticsService.generateMonthlyReport(userId, previousMonth);
                successCount++;

                notificationService.createNotification(
                        userId,
                        NotificationType.MONTHLY_SUMMARY,
                        previousMonth + " financial summary is ready",
                        String.format("Health score: %d/100 (%s). Savings rate: %.1f%%. Open the report for the full breakdown.",
                                report.getHealthScore(), report.getHealthScoreLabel(), report.getSavingsRatePercent()),
                        "MONTHLY_REPORT",
                        report.getId());

            } catch (Exception e) {
                // One user's failure must not stop the batch for everyone else.
                log.error("Failed to generate monthly report for userId={} for {}", userId, previousMonth, e);
            }
        }

        log.info("Monthly report generation complete: {}/{} succeeded for {}",
                successCount, userIds.size(), previousMonth);
    }
}