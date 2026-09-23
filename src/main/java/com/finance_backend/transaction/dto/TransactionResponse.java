package com.finance_backend.transaction.dto;

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
public class TransactionResponse {

    private Long sourceId;
    private TransactionType type;

    /** Always positive. */
    private BigDecimal amount;

    /** Expense amounts are negative and income amounts are positive, making totals easier to calculate. */
    private BigDecimal signedAmount;

    private String category;
    /** Merchant for expenses, income source label for income. */
    private String description;
    private LocalDate transactionDate;
    /** Only populated for EXPENSE rows. */
    private String paymentMethod;
    private String notes;
    private LocalDateTime createdAt;
}