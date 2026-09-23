package com.finance_backend.budget.dto;

import com.finance_backend.budget.entity.BudgetPeriod;
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
public class BudgetResponse {

    private Long id;
    private Long userId;
    private Long categoryId;
    /** Null for an overall budget. Resolved via CategoryRepository at read time. */
    private String categoryName;
    private BudgetPeriod period;
    private BigDecimal amount;
    private LocalDate startDate;
    /** Computed from startDate + period. */
    private LocalDate endDate;

    /** Computed by summing matching Expense entries in [startDate, endDate]. */
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private double percentUsed;
    private boolean alertTriggered;
    private Integer alertThresholdPercent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
