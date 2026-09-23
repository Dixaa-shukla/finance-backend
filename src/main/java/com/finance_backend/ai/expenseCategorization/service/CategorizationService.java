package com.finance_backend.ai.expenseCategorization.service;

import com.finance_backend.ai.expenseCategorization.dto.CategorizationConfirmRequest;
import com.finance_backend.ai.expenseCategorization.dto.CategorizationRequest;
import com.finance_backend.ai.expenseCategorization.dto.CategorizationResponse;

public interface CategorizationService {

    /** Checks the learned mapping table first; only calls the AI if nothing is learned yet for this merchant. */
    CategorizationResponse suggestCategory(CategorizationRequest request);

    /** Saves/updates the learned mapping -- call this when the user accepts or corrects a suggestion. */
    CategorizationResponse confirmCategorization(CategorizationConfirmRequest request);
}
