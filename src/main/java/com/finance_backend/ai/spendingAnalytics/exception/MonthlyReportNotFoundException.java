package com.finance_backend.ai.spendingAnalytics.exception;

import com.finance_backend.common.exception.ResourceNotFoundException;

import java.time.LocalDate;

public class MonthlyReportNotFoundException extends ResourceNotFoundException {

    public MonthlyReportNotFoundException(Long userId, LocalDate reportMonth) {
        super("No monthly report found for userId " + userId + " for " + reportMonth.getYear()
                + "-" + String.format("%02d", reportMonth.getMonthValue())
                + ". Generate one first via POST /monthly-report.");
    }
}
