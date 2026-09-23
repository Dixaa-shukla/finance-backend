package com.finance_backend.expense.dto;

import com.finance_backend.expense.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Inbound payload for creating/updating an expense.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "category is required")
    @Size(max = 50, message = "category must not exceed 50 characters")
    private String category;

    @Size(max = 150, message = "merchant must not exceed 150 characters")
    private String merchant;

    @NotNull(message = "expenseDate is required")
    @PastOrPresent(message = "expenseDate cannot be in the future")
    private LocalDate expenseDate;

    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    @Size(max = 500, message = "notes must not exceed 500 characters")
    private String notes;

    @Size(max = 500)
    private String receiptUrl;

    @Size(max = 150)
    private String location;

    private List<@Size(max = 30) String> tags;
}
