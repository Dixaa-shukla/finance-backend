package com.finance_backend.ai.expenseCategorization.controller;

import com.finance_backend.ai.expenseCategorization.dto.CategorizationConfirmRequest;
import com.finance_backend.ai.expenseCategorization.dto.CategorizationRequest;
import com.finance_backend.ai.expenseCategorization.dto.CategorizationResponse;
import com.finance_backend.ai.expenseCategorization.service.CategorizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/categorization")
public class CategorizationController {

    private final CategorizationService categorizationService;

    public CategorizationController(CategorizationService categorizationService) {
        this.categorizationService = categorizationService;
    }

    /**
     * Suggest a category + detected merchant for a raw expense description.
     * POST /api/v1/AI/categorization/suggest
     */
    @PostMapping("/suggest")
    public ResponseEntity<CategorizationResponse> suggestCategory(@Valid @RequestBody CategorizationRequest request) {
        return ResponseEntity.ok(categorizationService.suggestCategory(request));
    }

    /**
     * Confirm (or correct) a suggestion -- this is what actually teaches
     * the mapping table for next time. Call this whenever the user accepts
     * an AI suggestion as-is, or picks a different category than suggested.
     * POST /api/v1/Ai/categorization/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<CategorizationResponse> confirmCategorization(
            @Valid @RequestBody CategorizationConfirmRequest request) {
        return ResponseEntity.ok(categorizationService.confirmCategorization(request));
    }
}
