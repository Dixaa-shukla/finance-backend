package com.finance_backend.transaction.service;

import com.finance_backend.transaction.dto.TransactionFilterRequest;
import com.finance_backend.transaction.dto.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransactionService {

    /** Merged, filtered, sorted, paginated view over Expense + Income. */
    Page<TransactionResponse> getTransactionHistory(Long userId, TransactionFilterRequest filter, Pageable pageable);

    /** Same filter as history, but unpaginated -- all matching rows, for CSV export. */
    byte[] exportTransactionsAsCsv(Long userId, TransactionFilterRequest filter);

    /**
     * Module 8: Uses MySQL Full-Text Search to find expenses by merchant and notes.
     * It works alongside the existing LIKE search and needs a FULLTEXT index.
     */
    List<TransactionResponse> fullTextSearch(Long userId, String searchText);
}
