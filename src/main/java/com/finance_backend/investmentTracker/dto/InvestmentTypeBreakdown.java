package com.finance_backend.investmentTracker.dto;

import com.finance_backend.investmentTracker.entity.InvestmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Represents one row showing the details for a specific investment type. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentTypeBreakdown {

    private InvestmentType type;
    private int count;
    private BigDecimal investedAmount;
    private BigDecimal currentValue;
    private BigDecimal gainLossAmount;
    private double gainLossPercent;
}
