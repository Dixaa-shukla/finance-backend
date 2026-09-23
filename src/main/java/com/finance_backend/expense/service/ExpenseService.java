package com.finance_backend.expense.service;

import com.finance_backend.expense.dto.ExpenseFilterRequest;
import com.finance_backend.expense.dto.ExpenseRequest;
import com.finance_backend.expense.dto.ExpenseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExpenseService {

    ExpenseResponse createExpense(ExpenseRequest request);

    ExpenseResponse getExpenseById(Long id);

    List<ExpenseResponse> getExpensesByUserId(Long userId);

    Page<ExpenseResponse> searchExpenses(Long userId, ExpenseFilterRequest filter, Pageable pageable);

    ExpenseResponse updateExpense(Long id, ExpenseRequest request);

    void deleteExpense(Long id);
}