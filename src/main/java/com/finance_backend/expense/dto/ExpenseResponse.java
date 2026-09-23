package com.finance_backend.expense.dto;

import com.finance_backend.expense.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {

    private Long id;
    private Long userId;
    private BigDecimal amount;
    private String category;
    private String merchant;
    private LocalDate expenseDate;
    private PaymentMethod paymentMethod;
    private String notes;
    private String receiptUrl;
    private String location;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
