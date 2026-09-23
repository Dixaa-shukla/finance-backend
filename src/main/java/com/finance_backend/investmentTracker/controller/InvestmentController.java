package com.finance_backend.investmentTracker.controller;

import com.finance_backend.investmentTracker.dto.InvestmentRequest;
import com.finance_backend.investmentTracker.dto.InvestmentResponse;
import com.finance_backend.investmentTracker.dto.InvestmentSummaryResponse;
import com.finance_backend.investmentTracker.entity.InvestmentType;
import com.finance_backend.investmentTracker.service.InvestmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/investments")
public class InvestmentController {

    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    /**
     * Add a new investment (stock, mutual fund, SIP, gold, crypto, FD, PPF, EPF, NPS, bond).
     * POST /api/v1/investments
     */
    @PostMapping
    public ResponseEntity<InvestmentResponse> createInvestment(@Valid @RequestBody InvestmentRequest request) {
        InvestmentResponse response = investmentService.createInvestment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch a single investment by id.
     * GET /api/v1/investments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvestmentResponse> getInvestmentById(@PathVariable Long id) {
        return ResponseEntity.ok(investmentService.getInvestmentById(id));
    }

    /**
     * All investments for a user, optionally filtered by type, newest purchase first.
     * GET /api/v1/investments/user/{userId}?type=MUTUAL_FUND
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InvestmentResponse>> getInvestmentsByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) InvestmentType type) {
        return ResponseEntity.ok(investmentService.getInvestmentsByUserId(userId, type));
    }

    /**
     * Portfolio-level totals (invested, current value, gain/loss) with a
     * per-type breakdown -- e.g. how much is in Mutual Funds vs Gold vs FDs.
     * GET /api/v1/investments/user/{userId}/summary
     */
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<InvestmentSummaryResponse> getPortfolioSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(investmentService.getPortfolioSummary(userId));
    }

    /**
     * Updates an investment, such as changing its current value or recording withdrawals/top-ups.
     */
    @PutMapping("/{id}")
    public ResponseEntity<InvestmentResponse> updateInvestment(@PathVariable Long id,
                                                               @Valid @RequestBody InvestmentRequest request) {
        return ResponseEntity.ok(investmentService.updateInvestment(id, request));
    }

    /**
     * Delete an investment.
     * DELETE /api/v1/investments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvestment(@PathVariable Long id) {
        investmentService.deleteInvestment(id);
        return ResponseEntity.noContent().build();
    }
}
