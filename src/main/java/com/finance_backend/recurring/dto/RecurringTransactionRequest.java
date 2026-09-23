package com.finance_backend.recurring.dto;

import com.finance_backend.expense.entity.PaymentMethod;
import com.finance_backend.income.entity.IncomeSource;
import com.finance_backend.recurring.entity.RecurringFrequency;
import com.finance_backend.recurring.entity.RecurringTransactionType;
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
public class RecurringTransactionRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "type is required")
    private RecurringTransactionType type;

    @NotBlank(message = "title is required")
    @Size(max = 100, message = "title must not exceed 100 characters")
    private String title;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    /** Required if type = EXPENSE. */
    @Size(max = 50, message = "category must not exceed 50 characters")
    private String category;

    /** Required if type = INCOME. */
    private IncomeSource incomeSource;

    /** Optional; only used if type = EXPENSE. */
    private PaymentMethod paymentMethod;

    @NotNull(message = "frequency is required")
    private RecurringFrequency frequency;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    /** Optional; null means the rule runs indefinitely. */
    private LocalDate endDate;

    @Size(max = 500, message = "notes must not exceed 500 characters")
    private String notes;
}