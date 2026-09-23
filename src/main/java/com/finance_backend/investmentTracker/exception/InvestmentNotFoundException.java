package com.finance_backend.investmentTracker.exception;

import com.finance_backend.common.exception.ResourceNotFoundException;

public class InvestmentNotFoundException extends ResourceNotFoundException {

    public InvestmentNotFoundException(Long id) {
        super("Investment not found with id: " + id);
    }
}