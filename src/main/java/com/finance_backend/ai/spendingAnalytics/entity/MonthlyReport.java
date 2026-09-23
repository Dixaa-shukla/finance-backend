package com.finance_backend.ai.spendingAnalytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A persisted snapshot for one user, one month. Generated either by
 * MonthlyAnalyticsScheduler. reportMonth is always
 * stored as the first day of the month (LocalDate has no
 * native YearMonth column type in Hibernate without a custom converter).
 */
@Entity
@Table(name = "monthly_reports", uniqueConstraints = {
        @UniqueConstraint(name = "monthly_report_user_month", columnNames = {"user_id", "report_month"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Always the 1st of the reported month, e.g. 2026-07-01 for the July 2026 report. */
    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth;

    @Column(name = "total_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_expense", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalExpense;

    @Column(name = "savings_rate", nullable = false)
    private double savingsRate;

    @Column(name = "health_score", nullable = false)
    private int healthScore;

    /** AI-generated narrative summary. Falls back to a static sentence if the AI call fails. */
    @Column(name = "ai_insight", nullable = false, columnDefinition = "TEXT")
    private String aiInsight;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
