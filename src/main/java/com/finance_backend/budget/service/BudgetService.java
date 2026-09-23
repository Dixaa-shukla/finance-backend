package com.finance_backend.budget.service;

import com.finance_backend.budget.dto.BudgetRequest;
import com.finance_backend.budget.dto.BudgetResponse;

import java.util.List;

public interface BudgetService {

    BudgetResponse createBudget(BudgetRequest request);

    BudgetResponse getBudgetById(Long id);

    List<BudgetResponse> getBudgetsByUserId(Long userId);

    /** Budgets for a user where current spend has crossed alertThresholdPercent. */
    List<BudgetResponse> getTriggeredAlertsForUser(Long userId);

    BudgetResponse updateBudget(Long id, BudgetRequest request);

    void deleteBudget(Long id);
}
