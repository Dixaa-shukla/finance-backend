package com.finance_backend.ai.spendingAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Live, on-demand health score . Sub-scores are exposed alongside the total
 * so the score is explainable, not a black box.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthScoreResponse {

    private Long userId;
    /** 0-100 overall score. */
    private int score;
    /** "Excellent" / "Good" / "Fair" / "Needs Attention". */
    private String label;

    // Sub-scores, for transparency:
    private double savingsRatePercent;
    private double savingsScoreOutOf40;
    private double budgetAdherencePercent;
    private double budgetScoreOutOf30;
    private double goalOnTrackPercent;
    private double goalScoreOutOf30;
}
