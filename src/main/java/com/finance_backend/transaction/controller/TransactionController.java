package com.finance_backend.transaction.controller;

import com.finance_backend.transaction.dto.TransactionFilterRequest;
import com.finance_backend.transaction.dto.TransactionResponse;
import com.finance_backend.transaction.dto.TransactionType;
import com.finance_backend.transaction.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Shows combined Expense and Income transactions with filters, sorting, and pagination.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @PageableDefault(size = 20, sort = "transactionDate") Pageable pageable) {

        TransactionFilterRequest filter = TransactionFilterRequest.builder()
                .type(type)
                .category(category)
                .startDate(startDate)
                .endDate(endDate)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .build();

        return ResponseEntity.ok(transactionService.getTransactionHistory(userId, filter, pageable));
    }

    /**
     * Downloads the same filtered set as a CSV file. Same query params as
     * above (minus paging -- export always returns everything that matches).
     * GET /api/v1/transactions/user/{userId}/export?type=EXPENSE&startDate=2026-08-01&endDate=2026-08-31
     */
    @GetMapping("/user/{userId}/export")
    public ResponseEntity<byte[]> exportTransactions(
            @PathVariable Long userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {

        TransactionFilterRequest filter = TransactionFilterRequest.builder()
                .type(type)
                .category(category)
                .startDate(startDate)
                .endDate(endDate)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .build();

        byte[] csv = transactionService.exportTransactionsAsCsv(userId, filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

     /**
     * Module 8: Uses MySQL Full-Text Search to search expense merchant names and notes.
     * It provides better matching than the normal LIKE search and requires a FULLTEXT index.
     * GET /api/v1/transactions/user/{userId}/fulltext-search?q=domino
     */
    @GetMapping("/user/{userId}/fulltext-search")
    public ResponseEntity<List<TransactionResponse>> fullTextSearch(
            @PathVariable Long userId,
            @RequestParam String q) {
        return ResponseEntity.ok(transactionService.fullTextSearch(userId, q));
    }
}
