package com.finance_backend.ai.spendingAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendingTrendResponse {

    private Long userId;
    /** Oldest month first. */
    private List<MonthlySpendingBreakdown> months;
}
