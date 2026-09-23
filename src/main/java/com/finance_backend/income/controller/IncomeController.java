package com.finance_backend.income.controller;

import com.finance_backend.income.dto.IncomeFilterRequest;
import com.finance_backend.income.dto.IncomeRequest;
import com.finance_backend.income.dto.IncomeResponse;
import com.finance_backend.income.entity.IncomeSource;
import com.finance_backend.income.service.IncomeService;
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
@RequestMapping("/api/v1/incomes")
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    /**
     * Adding a new income entry.
     * POST /api/v1/incomes
     */
    @PostMapping
    public ResponseEntity<IncomeResponse> createIncome(@Valid @RequestBody IncomeRequest request) {
        IncomeResponse response = incomeService.createIncome(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch a single income entry by id.
     * GET /api/v1/incomes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncomeResponse> getIncomeById(@PathVariable Long id) {
        return ResponseEntity.ok(incomeService.getIncomeById(id));
    }

    /**
     * All income entries for a user, newest first, unpaginated.
     * GET /api/v1/incomes/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<IncomeResponse>> getIncomesByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(incomeService.getIncomesByUserId(userId));
    }

    /**
     * Search & filter a user's income with pagination.
     * GET /api/v1/incomes/user/{userId}/search?source=SALARY&categoryId=3
     *     &startDate=2026-01-01&endDate=2026-01-31&minAmount=1000&maxAmount=100000
     *     &isRecurring=true&page=0&size=10&sort=incomeDate,desc
     */
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<Page<IncomeResponse>> searchIncomes(
            @PathVariable Long userId,
            @RequestParam(required = false) IncomeSource source,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) Boolean isRecurring,
            @PageableDefault(size = 10, sort = "incomeDate") Pageable pageable) {

        IncomeFilterRequest filter = IncomeFilterRequest.builder()
                .source(source)
                .categoryId(categoryId)
                .startDate(startDate)
                .endDate(endDate)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .isRecurring(isRecurring)
                .build();

        return ResponseEntity.ok(incomeService.searchIncomes(userId, filter, pageable));
    }

    /**
     * Edit an existing income entry.
     * PUT /api/v1/incomes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<IncomeResponse> updateIncome(@PathVariable Long id,
                                                       @Valid @RequestBody IncomeRequest request) {
        return ResponseEntity.ok(incomeService.updateIncome(id, request));
    }

    /**
     * Delete an income entry.
     * DELETE /api/v1/incomes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id) {
        incomeService.deleteIncome(id);
        return ResponseEntity.noContent().build();
    }
}
