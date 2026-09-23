package com.finance_backend.investmentTracker.dto;

import com.finance_backend.investmentTracker.entity.InvestmentType;
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
public class InvestmentResponse {

    private Long id;
    private Long userId;
    private InvestmentType type;
    private String name;
    private BigDecimal investedAmount;
    private BigDecimal currentValue;
    private BigDecimal quantity;
    private LocalDate purchaseDate;
    private LocalDate maturityDate;
    private BigDecimal interestRate;
    private String notes;

    /** currentValue - investedAmount. Negative means a loss. */
    private BigDecimal gainLossAmount;
    private double gainLossPercent;
    /** True if maturityDate is set and has already passed. */
    private boolean matured;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
