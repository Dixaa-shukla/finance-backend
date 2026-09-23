package com.finance_backend.expense.service;

import com.finance_backend.expense.dto.ExpenseFilterRequest;
import com.finance_backend.expense.dto.ExpenseRequest;
import com.finance_backend.expense.dto.ExpenseResponse;
import com.finance_backend.expense.entity.Expense;
import com.finance_backend.expense.exception.ExpenseNotFoundException;
import com.finance_backend.expense.repository.ExpenseRepository;
import com.finance_backend.expense.repository.ExpenseSpecification;
import com.finance_backend.auth.security.ResourceOwnershipGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ResourceOwnershipGuard ownershipGuard;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository,
                              ResourceOwnershipGuard ownershipGuard) {
        this.expenseRepository = expenseRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @Override
    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        Expense expense = toEntity(request, new Expense());
        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
        ownershipGuard.check(expense.getUserId(), "expense", id);
        return toResponse(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByUserId(Long userId) {
        return expenseRepository.findByUserIdOrderByExpenseDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> searchExpenses(Long userId, ExpenseFilterRequest filter, Pageable pageable) {
        return expenseRepository
                .findAll(ExpenseSpecification.byUserAndFilters(userId, filter), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
        /*
         * ⚠️ BEFORE toEntity(), NOT AFTER. toEntity overwrites userId from the
         * request body, so checking afterwards would read the CALLER as the owner
         * and always pass -- quietly moving someone else's expense onto your
         * account rather than refusing the edit.
         */
        ownershipGuard.check(existing.getUserId(), "expense", id);
        Expense updated = toEntity(request, existing);
        Expense saved = expenseRepository.save(updated);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        // findById, not existsById: the row has to be loaded to know who owns it.
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
        ownershipGuard.check(expense.getUserId(), "expense", id);
        expenseRepository.delete(expense);
    }

    // ---------- mapping helpers ----------

    private Expense toEntity(ExpenseRequest request, Expense target) {
        target.setUserId(request.getUserId());
        target.setAmount(request.getAmount());
        target.setCategory(request.getCategory());
        target.setMerchant(request.getMerchant());
        target.setExpenseDate(request.getExpenseDate());
        target.setPaymentMethod(request.getPaymentMethod());
        target.setNotes(request.getNotes());
        target.setReceiptUrl(request.getReceiptUrl());
        target.setLocation(request.getLocation());
        if (request.getTags() != null) {
            target.setTags(request.getTags());
        }
        return target;
    }

    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .userId(expense.getUserId())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .merchant(expense.getMerchant())
                .expenseDate(expense.getExpenseDate())
                .paymentMethod(expense.getPaymentMethod())
                .notes(expense.getNotes())
                .receiptUrl(expense.getReceiptUrl())
                .location(expense.getLocation())
                .tags(expense.getTags())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}