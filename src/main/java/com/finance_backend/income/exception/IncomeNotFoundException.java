package com.finance_backend.income.exception;

import com.finance_backend.common.exception.ResourceNotFoundException;

public class IncomeNotFoundException extends ResourceNotFoundException {

    public IncomeNotFoundException(Long id) {
        super("Income not found with id: " + id);
    }
}