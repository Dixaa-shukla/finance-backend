package com.finance_backend.budget.dto;

import com.finance_backend.budget.entity.BudgetPeriod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    /** Optional; if provided, must reference an existing category of type EXPENSE. */
    private Long categoryId;

    @NotNull(message = "period is required")
    private BudgetPeriod period;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @Min(value = 1, message = "alertThresholdPercent must be between 1 and 100")
    @Max(value = 100, message = "alertThresholdPercent must be between 1 and 100")
    @Builder.Default
    private Integer alertThresholdPercent = 80;
}
