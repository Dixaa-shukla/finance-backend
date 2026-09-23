package com.finance_backend.expense.exception;

import com.finance_backend.common.exception.ResourceNotFoundException;

public class ExpenseNotFoundException extends ResourceNotFoundException {

    public ExpenseNotFoundException(Long id) {
        super("Expense not found with id: " + id);
    }
}
