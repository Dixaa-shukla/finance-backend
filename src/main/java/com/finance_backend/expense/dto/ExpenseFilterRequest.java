package com.finance_backend.expense.dto;

import com.finance_backend.expense.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * All fields are optional and come from search query parameters.
 * Any combination can be used, and empty fields will simply be ignored.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseFilterRequest {

    private String category;
    private PaymentMethod paymentMethod;
    /** Partial, case-insensitive match against merchant name. */
    private String merchant;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String tag;
}
