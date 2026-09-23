package com.finance_backend.goal.dto;

import com.finance_backend.goal.entity.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalResponse {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private double progressPercent;
    private LocalDate targetDate;
    /** Negative once the deadline has passed. */
    private long daysRemaining;
    private GoalStatus status;
    /** Calculates remaining amount divided equally across months left; returns null if the deadline has passed. */
    private BigDecimal suggestedMonthlySaving;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
