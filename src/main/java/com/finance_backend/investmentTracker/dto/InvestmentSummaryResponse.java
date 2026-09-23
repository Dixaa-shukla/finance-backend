package com.finance_backend.investmentTracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentSummaryResponse {

    private Long userId;
    private BigDecimal totalInvested;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalGainLossAmount;
    private double totalGainLossPercent;
    private int totalInvestmentCount;
    private List<InvestmentTypeBreakdown> breakdownByType;
}