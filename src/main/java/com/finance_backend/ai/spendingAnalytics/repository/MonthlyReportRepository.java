package com.finance_backend.ai.spendingAnalytics.repository;

import com.finance_backend.ai.spendingAnalytics.entity.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {

    Optional<MonthlyReport> findByUserIdAndReportMonth(Long userId, LocalDate reportMonth);

    List<MonthlyReport> findByUserIdOrderByReportMonthDesc(Long userId);

    /**
     * Added for Module 16 (Admin Panel): how many AI-generated monthly reports
     * exist for one user -- part of that user's AI footprint in the admin
     * detail view.
     */
    long countByUserId(Long userId);
}

