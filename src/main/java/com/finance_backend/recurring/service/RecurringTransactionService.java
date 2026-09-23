package com.finance_backend.recurring.service;

import com.finance_backend.recurring.dto.RecurringTransactionRequest;
import com.finance_backend.recurring.dto.RecurringTransactionResponse;

import java.util.List;

public interface RecurringTransactionService {

    RecurringTransactionResponse createRecurringTransaction(RecurringTransactionRequest request);

    RecurringTransactionResponse getRecurringTransactionById(Long id);

    List<RecurringTransactionResponse> getRecurringTransactionsByUserId(Long userId);

    RecurringTransactionResponse updateRecurringTransaction(Long id, RecurringTransactionRequest request);

    RecurringTransactionResponse pauseRecurringTransaction(Long id);

    RecurringTransactionResponse resumeRecurringTransaction(Long id);

    void deleteRecurringTransaction(Long id);

    /**
     * Creates Expense/Income records for all active recurring transactions that are due.
     * It also handles missed cycles and updates the next due date.
     */
    int processDueRecurringTransactions();
}
