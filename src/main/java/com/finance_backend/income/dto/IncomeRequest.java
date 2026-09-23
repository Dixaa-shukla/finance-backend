package com.finance_backend.income.dto;

import com.finance_backend.income.entity.IncomeSource;
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
public class IncomeRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "source is required")
    private IncomeSource source;

    /** Optional; if provided, must reference an existing category of type INCOME. */
    private Long categoryId;

    @NotNull(message = "incomeDate is required")
    @PastOrPresent(message = "incomeDate cannot be in the future")
    private LocalDate incomeDate;

    @Size(max = 500, message = "notes must not exceed 500 characters")
    private String notes;

    @Builder.Default
    private boolean isRecurring = false;
}