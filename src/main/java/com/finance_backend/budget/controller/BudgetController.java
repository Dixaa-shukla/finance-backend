package com.finance_backend.budget.controller;

import com.finance_backend.budget.dto.BudgetRequest;
import com.finance_backend.budget.dto.BudgetResponse;
import com.finance_backend.budget.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * Create a new budget (overall or per-category, daily/weekly/monthly).
     * POST /api/v1/budgets
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.createBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch a single budget by id, with live spend/remaining/percentUsed computed.
     * GET /api/v1/budgets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getBudgetById(id));
    }

    /**
     * All budgets for a user, each with live spend data computed.
     * GET /api/v1/budgets/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BudgetResponse>> getBudgetsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(budgetService.getBudgetsByUserId(userId));
    }

    /**
     * Budgets for a user that have crossed their alert threshold right now.
     * GET /api/v1/budgets/user/{userId}/alerts
     */
    @GetMapping("/user/{userId}/alerts")
    public ResponseEntity<List<BudgetResponse>> getTriggeredAlerts(@PathVariable Long userId) {
        return ResponseEntity.ok(budgetService.getTriggeredAlertsForUser(userId));
    }

    /**
     * Update an existing budget.
     * PUT /api/v1/budgets/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(@PathVariable Long id,
                                                       @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.updateBudget(id, request));
    }

    /**
     * Delete a budget.
     * DELETE /api/v1/budgets/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
