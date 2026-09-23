package com.finance_backend.ai.financialAssistant.service;

import com.finance_backend.expense.dto.ExpenseResponse;
import com.finance_backend.expense.service.ExpenseService;
import com.finance_backend.income.dto.IncomeResponse;
import com.finance_backend.income.service.IncomeService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds one Document per Expense/Income row with a deterministic id.
 * Reindexing replaces the user's complete vector set, so deleted transactions
 * cannot remain available to semantic search.
 */
@Service
public class TransactionIndexingServiceImpl implements TransactionIndexingService {

    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final VectorStore vectorStore;
    private final String persistPath;

    public TransactionIndexingServiceImpl(ExpenseService expenseService,
                                          IncomeService incomeService,
                                          VectorStore vectorStore,
                                          org.springframework.core.env.Environment env) {
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.vectorStore = vectorStore;
        this.persistPath = env.getProperty("finance.ai.vector-store.persist-path", "./data/vector-store.json");
    }

    @Override
    @Transactional(readOnly = true)
    public int reindexUser(Long userId) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByUserId(userId);
        List<IncomeResponse> incomes = incomeService.getIncomesByUserId(userId);

        List<Document> documents = new ArrayList<>();

        for (ExpenseResponse e : expenses) {
            String id = "expense-" + e.getId();
            documents.add(new Document(id, buildExpenseText(e), Map.of(
                    "userId", userId,
                    "type", "EXPENSE",
                    "sourceId", e.getId(),
                    "date", e.getExpenseDate().toString()
            )));
        }

        for (IncomeResponse i : incomes) {
            String id = "income-" + i.getId();
            documents.add(new Document(id, buildIncomeText(i), Map.of(
                    "userId", userId,
                    "type", "INCOME",
                    "sourceId", i.getId(),
                    "date", i.getIncomeDate().toString()
            )));
        }

        // Replace the user's full set, including vectors for records deleted
        // since the previous index operation.
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        vectorStore.delete(filter.eq("userId", userId).build());
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }

        persistToDisk();

        return documents.size();
    }

    private String buildExpenseText(ExpenseResponse e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Expense: Rs.").append(e.getAmount())
                .append(" on ").append(e.getCategory());
        if (e.getMerchant() != null) {
            sb.append(" at ").append(e.getMerchant());
        }
        sb.append(" on ").append(e.getExpenseDate());
        if (e.getNotes() != null && !e.getNotes().isBlank()) {
            sb.append(". Notes: ").append(e.getNotes());
        }
        return sb.toString();
    }

    private String buildIncomeText(IncomeResponse i) {
        StringBuilder sb = new StringBuilder();
        sb.append("Income: Rs.").append(i.getAmount())
                .append(" from ").append(i.getSource())
                .append(" on ").append(i.getIncomeDate());
        if (i.getNotes() != null && !i.getNotes().isBlank()) {
            sb.append(". Notes: ").append(i.getNotes());
        }
        return sb.toString();
    }

    private void persistToDisk() {
        try {
            if (vectorStore instanceof org.springframework.ai.vectorstore.SimpleVectorStore simpleStore) {
                File file = new File(persistPath);
                Path parent = file.toPath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                simpleStore.save(file);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist vector store to disk at " + persistPath, e);
        }
    }
}
