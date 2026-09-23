package com.finance_backend.budget.exception;

import com.finance_backend.common.exception.ResourceNotFoundException;

public class BudgetNotFoundException extends ResourceNotFoundException {

    public BudgetNotFoundException(Long id) {
        super("Budget not found with id: " + id);
    }
}
