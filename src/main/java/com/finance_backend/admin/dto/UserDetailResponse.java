package com.finance_backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {

    private Long userId;
    private String fullName;
    private String phoneNumber;
    private String preferredCurrency;
    private String primaryFinancialGoal;
    private BigDecimal monthlySalary;
    private String profilePictureUrl;

    // ---- financial footprint ----
    private long expenseCount;
    private long incomeCount;
    private BigDecimal totalSpent;
    private BigDecimal totalEarned;
    private BigDecimal netSavings;

    // ---- AI footprint ----
    private long aiChatMessageCount;
    private long learnedMerchantCount;
    private long monthlyReportCount;

    // ---- health (reused from AnalyticsService) ----
    private int healthScore;
    private String healthScoreLabel;

    private LocalDateTime joinedAt;
    private LocalDateTime lastUpdatedAt;
}
