package com.finance_backend.recurring.dto;

import com.finance_backend.expense.entity.PaymentMethod;
import com.finance_backend.income.entity.IncomeSource;
import com.finance_backend.recurring.entity.RecurringFrequency;
import com.finance_backend.recurring.entity.RecurringTransactionType;
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
public class RecurringTransactionResponse {

    private Long id;
    private Long userId;
    private RecurringTransactionType type;
    private String title;
    private BigDecimal amount;
    private String category;
    private IncomeSource incomeSource;
    private PaymentMethod paymentMethod;
    private RecurringFrequency frequency;
    private LocalDate startDate;
    private LocalDate nextDueDate;
    private LocalDate endDate;
    private boolean isActive;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
