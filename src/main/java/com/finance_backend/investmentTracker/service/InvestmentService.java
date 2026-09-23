package com.finance_backend.investmentTracker.service;

import com.finance_backend.investmentTracker.dto.InvestmentRequest;
import com.finance_backend.investmentTracker.dto.InvestmentResponse;
import com.finance_backend.investmentTracker.dto.InvestmentSummaryResponse;
import com.finance_backend.investmentTracker.entity.InvestmentType;

import java.util.List;

public interface InvestmentService {

    InvestmentResponse createInvestment(InvestmentRequest request);

    InvestmentResponse getInvestmentById(Long id);

    List<InvestmentResponse> getInvestmentsByUserId(Long userId, InvestmentType type);

    InvestmentSummaryResponse getPortfolioSummary(Long userId);

    InvestmentResponse updateInvestment(Long id, InvestmentRequest request);

    void deleteInvestment(Long id);
}
