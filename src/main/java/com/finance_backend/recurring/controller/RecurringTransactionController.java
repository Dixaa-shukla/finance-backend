package com.finance_backend.recurring.controller;

import com.finance_backend.recurring.dto.RecurringTransactionRequest;
import com.finance_backend.recurring.dto.RecurringTransactionResponse;
import com.finance_backend.recurring.service.RecurringTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recurring-transactions")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionController(RecurringTransactionService recurringTransactionService) {
        this.recurringTransactionService = recurringTransactionService;
    }

    /**
     * Create a new recurring rule (rent, EMI, subscription, etc.).
     * POST /api/v1/recurring-transactions
     */
    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> createRecurringTransaction(
            @Valid @RequestBody RecurringTransactionRequest request) {
        RecurringTransactionResponse response = recurringTransactionService.createRecurringTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch a single recurring rule by id.
     * GET /api/v1/recurring-transactions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> getRecurringTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(recurringTransactionService.getRecurringTransactionById(id));
    }

    /**
     * All recurring rules for a user.
     * GET /api/v1/recurring-transactions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecurringTransactionResponse>> getRecurringTransactionsByUserId(
            @PathVariable Long userId) {
        return ResponseEntity.ok(recurringTransactionService.getRecurringTransactionsByUserId(userId));
    }

    /**
     * Edit a recurring rule's amount/title/category/etc.
     * Does NOT reset nextDueDate -- the schedule keeps running from where it was.
     * PUT /api/v1/recurring-transactions/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> updateRecurringTransaction(
            @PathVariable Long id, @Valid @RequestBody RecurringTransactionRequest request) {
        return ResponseEntity.ok(recurringTransactionService.updateRecurringTransaction(id, request));
    }

    /**
     * Pause a rule without deleting it -- the scheduler will skip it.
     * PATCH /api/v1/recurring-transactions/{id}/pause
     */
    @PatchMapping("/{id}/pause")
    public ResponseEntity<RecurringTransactionResponse> pauseRecurringTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(recurringTransactionService.pauseRecurringTransaction(id));
    }

    /**
     * Resume a paused rule.
     * PATCH /api/v1/recurring-transactions/{id}/resume
     */
    @PatchMapping("/{id}/resume")
    public ResponseEntity<RecurringTransactionResponse> resumeRecurringTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(recurringTransactionService.resumeRecurringTransaction(id));
    }

    /**
     * Delete a recurring rule. Does NOT delete previously generated Expense/Income rows.
     * DELETE /api/v1/recurring-transactions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringTransaction(@PathVariable Long id) {
        recurringTransactionService.deleteRecurringTransaction(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manually runs the recurring transaction process for testing.
     * This is admin-only because it processes transactions for all users.
     */
    @PostMapping("/process-due")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> processDueRecurringTransactions() {
        int generated = recurringTransactionService.processDueRecurringTransactions();
        return ResponseEntity.ok(Map.of("transactionsGenerated", generated));
    }
}