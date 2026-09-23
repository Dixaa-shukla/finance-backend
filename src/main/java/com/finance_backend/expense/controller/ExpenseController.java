package com.finance_backend.expense.controller;

import com.finance_backend.expense.dto.ExpenseFilterRequest;
import com.finance_backend.expense.dto.ExpenseRequest;
import com.finance_backend.expense.dto.ExpenseResponse;
import com.finance_backend.expense.entity.PaymentMethod;
import com.finance_backend.expense.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /**
     * Add a new expense.
     * POST /api/v1/expenses
     */
    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch a single expense by id.
     * GET /api/v1/expenses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    /**
     * All expenses for a user, newest first.
     * GET /api/v1/expenses/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(expenseService.getExpensesByUserId(userId));
    }

    /**
     * Search & filter a user's expenses.
     * GET /api/v1/expenses/user/{userId}/search?category=Food&paymentMethod=UPI
     *     &merchant=domino&startDate=2026-01-01&endDate=2026-01-31
     *     &minAmount=100&maxAmount=5000&tag=recurring&page=0&size=10&sort=expenseDate,desc
     */
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<Page<ExpenseResponse>> searchExpenses(
            @PathVariable Long userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 10, sort = "expenseDate") Pageable pageable) {

        ExpenseFilterRequest filter = ExpenseFilterRequest.builder()
                .category(category)
                .paymentMethod(paymentMethod)
                .merchant(merchant)
                .startDate(startDate)
                .endDate(endDate)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .tag(tag)
                .build();

        return ResponseEntity.ok(expenseService.searchExpenses(userId, filter, pageable));
    }

    /**
     * Edit an existing expense.
     * PUT /api/v1/expenses/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable Long id,
                                                         @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request));
    }

    /**
     * Delete an expense.
     * DELETE /api/v1/expenses/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}
