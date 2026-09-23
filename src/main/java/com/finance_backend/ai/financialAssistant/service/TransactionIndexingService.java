package com.finance_backend.ai.financialAssistant.service;

public interface TransactionIndexingService {

    /**
     * (Re)indexes every current Expense and Income record for a user into
     * the vector store, so the assistant can semantically search individual
     * transactions, not just the aggregate summary.
     *
     * @return the number of documents indexed.
     */
    int reindexUser(Long userId);
}
