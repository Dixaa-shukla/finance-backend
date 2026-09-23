package com.finance_backend.transaction.service;

import com.finance_backend.category.entity.Category;
import com.finance_backend.category.repository.CategoryRepository;
import com.finance_backend.expense.entity.Expense;
import com.finance_backend.expense.repository.ExpenseRepository;
import com.finance_backend.income.entity.Income;
import com.finance_backend.income.repository.IncomeRepository;
import com.finance_backend.transaction.dto.TransactionFilterRequest;
import com.finance_backend.transaction.dto.TransactionResponse;
import com.finance_backend.transaction.dto.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aggregates Expense + Income into a single read-only transaction view.
 * No own table -- see the module's design rationale for why.
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;

    public TransactionServiceImpl(ExpenseRepository expenseRepository,
                                  IncomeRepository incomeRepository,
                                  CategoryRepository categoryRepository) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(Long userId, TransactionFilterRequest filter, Pageable pageable) {
        List<TransactionResponse> filtered = fetchFilteredAndSorted(userId, filter, pageable.getSort());

        int start = (int) pageable.getOffset();
        if (start >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<TransactionResponse> pageContent = filtered.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTransactionsAsCsv(Long userId, TransactionFilterRequest filter) {
        List<TransactionResponse> transactions = fetchFilteredAndSorted(
                userId, filter, Sort.by(Sort.Direction.DESC, "transactionDate"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("Type,Date,Category,Description,PaymentMethod,Amount,Notes");
            for (TransactionResponse t : transactions) {
                writer.println(String.join(",",
                        csv(t.getType().name()),
                        csv(t.getTransactionDate().toString()),
                        csv(t.getCategory()),
                        csv(t.getDescription()),
                        csv(t.getPaymentMethod()),
                        csv(t.getAmount().toPlainString()),
                        csv(t.getNotes())
                ));
            }
        }
        return out.toByteArray();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> fullTextSearch(Long userId, String searchText) {
        return expenseRepository.fullTextSearch(userId, searchText)
                .stream()
                .map(this::fromExpense)
                .toList();
    }

    // ---------- helpers ----------

    private List<TransactionResponse> fetchFilteredAndSorted(Long userId, TransactionFilterRequest filter, Sort sort) {
        List<TransactionResponse> all = new ArrayList<>();

        if (filter == null || filter.getType() == null || filter.getType() == TransactionType.EXPENSE) {
            expenseRepository.findByUserIdOrderByExpenseDateDesc(userId)
                    .forEach(e -> all.add(fromExpense(e)));
        }
        if (filter == null || filter.getType() == null || filter.getType() == TransactionType.INCOME) {
            incomeRepository.findByUserIdOrderByIncomeDateDesc(userId)
                    .forEach(i -> all.add(fromIncome(i)));
        }

        List<TransactionResponse> filtered = all.stream()
                .filter(t -> matchesFilter(t, filter))
                .sorted(buildComparator(sort))
                .toList();

        return filtered;
    }

    private boolean matchesFilter(TransactionResponse t, TransactionFilterRequest filter) {
        if (filter == null) {
            return true;
        }
        if (StringUtils.hasText(filter.getCategory())
                && (t.getCategory() == null || !t.getCategory().toLowerCase().contains(filter.getCategory().toLowerCase()))) {
            return false;
        }
        if (filter.getStartDate() != null && t.getTransactionDate().isBefore(filter.getStartDate())) {
            return false;
        }
        if (filter.getEndDate() != null && t.getTransactionDate().isAfter(filter.getEndDate())) {
            return false;
        }
        if (filter.getMinAmount() != null && t.getAmount().compareTo(filter.getMinAmount()) < 0) {
            return false;
        }
        if (filter.getMaxAmount() != null && t.getAmount().compareTo(filter.getMaxAmount()) > 0) {
            return false;
        }
        return true;
    }

    /**
     * Supports sorting by transaction date or amount.
     * If no valid sort is given, it defaults to transaction date descending.
     */
    private Comparator<TransactionResponse> buildComparator(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Comparator.comparing(TransactionResponse::getTransactionDate).reversed();
        }

        Sort.Order order = sort.iterator().next();
        Comparator<TransactionResponse> comparator = switch (order.getProperty()) {
            case "amount" -> Comparator.comparing(TransactionResponse::getAmount);
            default -> Comparator.comparing(TransactionResponse::getTransactionDate);
        };

        return order.isDescending() ? comparator.reversed() : comparator;
    }

    private TransactionResponse fromExpense(Expense expense) {
        return TransactionResponse.builder()
                .sourceId(expense.getId())
                .type(TransactionType.EXPENSE)
                .amount(expense.getAmount())
                .signedAmount(expense.getAmount().negate())
                .category(expense.getCategory())
                .description(expense.getMerchant())
                .transactionDate(expense.getExpenseDate())
                .paymentMethod(expense.getPaymentMethod() != null ? expense.getPaymentMethod().name() : null)
                .notes(expense.getNotes())
                .createdAt(expense.getCreatedAt())
                .build();
    }

    private TransactionResponse fromIncome(Income income) {
        String categoryName = null;
        if (income.getCategoryId() != null) {
            categoryName = categoryRepository.findById(income.getCategoryId())
                    .map(Category::getName)
                    .orElse(null);
        }
        if (categoryName == null) {
            categoryName = income.getSource().name();
        }

        return TransactionResponse.builder()
                .sourceId(income.getId())
                .type(TransactionType.INCOME)
                .amount(income.getAmount())
                .signedAmount(income.getAmount())
                .category(categoryName)
                .description(income.getSource().name())
                .transactionDate(income.getIncomeDate())
                .paymentMethod(null)
                .notes(income.getNotes())
                .createdAt(income.getCreatedAt())
                .build();
    }

    /** Minimal CSV escaping: wraps in quotes and doubles any internal quotes if a comma/quote/newline is present. */
    private String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}