package com.finance_backend.recurring.exception;

import com.finance_backend.common.exception.ResourceNotFoundException;

public class RecurringTransactionNotFoundException extends ResourceNotFoundException {

    public RecurringTransactionNotFoundException(Long id) {
        super("Recurring transaction not found with id: " + id);
    }
}
