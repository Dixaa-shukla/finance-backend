package com.finance_backend.budget.service;

import com.finance_backend.budget.dto.BudgetRequest;
import com.finance_backend.budget.dto.BudgetResponse;
import com.finance_backend.budget.entity.Budget;
import com.finance_backend.budget.entity.BudgetPeriod;
import com.finance_backend.budget.exception.BudgetNotFoundException;
import com.finance_backend.budget.repository.BudgetRepository;
import com.finance_backend.auth.security.ResourceOwnershipGuard;
import com.finance_backend.category.entity.Category;
import com.finance_backend.category.entity.CategoryType;
import com.finance_backend.category.repository.CategoryRepository;
import com.finance_backend.common.exception.BadRequestException;
import com.finance_backend.expense.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final ResourceOwnershipGuard ownershipGuard;

    public BudgetServiceImpl(BudgetRepository budgetRepository,
                             CategoryRepository categoryRepository,
                             ExpenseRepository expenseRepository,
                             ResourceOwnershipGuard ownershipGuard) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @Override
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        validateCategory(request.getCategoryId());
        Budget budget = toEntity(request, new Budget());
        Budget saved = budgetRepository.save(budget);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));
        ownershipGuard.check(budget.getUserId(), "budget", id);
        return toResponse(budget);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByUserId(Long userId) {
        return budgetRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getTriggeredAlertsForUser(Long userId) {
        return budgetRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .filter(BudgetResponse::isAlertTriggered)
                .toList();
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        validateCategory(request.getCategoryId());
        Budget existing = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));
        /*
         * ⚠️ CHECKED BEFORE toEntity(), NOT AFTER. toEntity overwrites userId from
         * the request body, so afterwards `existing` reports the CALLER as its
         * owner and the check would always pass -- silently reassigning someone
         * else's budget instead of refusing.
         */
        ownershipGuard.check(existing.getUserId(), "budget", id);
        Budget updated = toEntity(request, existing);
        Budget saved = budgetRepository.save(updated);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteBudget(Long id) {
        /*
         * findById instead of existsById: the row has to be LOADED to know who
         * owns it. existsById could only answer "does this id exist", which is
         * exactly the question that let anyone delete anyone's budget.
         */
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));
        ownershipGuard.check(budget.getUserId(), "budget", id);
        budgetRepository.delete(budget);
    }

    // ---------- helpers ----------

    /** If a categoryId is supplied, it must exist and be typed EXPENSE. */
    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BadRequestException("categoryId does not reference an existing category: " + categoryId));

        if (category.getType() != CategoryType.EXPENSE) {
            throw new BadRequestException("categoryId " + categoryId + " is not an EXPENSE category");
        }
    }

    private Budget toEntity(BudgetRequest request, Budget target) {
        target.setUserId(request.getUserId());
        target.setCategoryId(request.getCategoryId());
        target.setPeriod(request.getPeriod());
        target.setAmount(request.getAmount());
        target.setStartDate(request.getStartDate());
        target.setAlertThresholdPercent(request.getAlertThresholdPercent());
        return target;
    }

    private LocalDate computeEndDate(LocalDate startDate, BudgetPeriod period) {
        return switch (period) {
            case DAILY -> startDate;
            case WEEKLY -> startDate.plusDays(6);
            case MONTHLY -> startDate.plusMonths(1).minusDays(1);
        };
    }

    private BudgetResponse toResponse(Budget budget) {
        LocalDate endDate = computeEndDate(budget.getStartDate(), budget.getPeriod());

        String categoryName = null;
        BigDecimal spent;

        if (budget.getCategoryId() != null) {
            Optional<Category> category = categoryRepository.findById(budget.getCategoryId());
            categoryName = category.map(Category::getName).orElse(null);
            spent = expenseRepository.sumAmountByUserAndDateRangeAndCategory(
                    budget.getUserId(), budget.getStartDate(), endDate, categoryName != null ? categoryName : "");
        } else {
            spent = expenseRepository.sumAmountByUserAndDateRange(
                    budget.getUserId(), budget.getStartDate(), endDate);
        }

        BigDecimal remaining = budget.getAmount().subtract(spent);
        double percentUsed = spent
                .divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        boolean alertTriggered = percentUsed >= budget.getAlertThresholdPercent();

        return BudgetResponse.builder()
                .id(budget.getId())
                .userId(budget.getUserId())
                .categoryId(budget.getCategoryId())
                .categoryName(categoryName)
                .period(budget.getPeriod())
                .amount(budget.getAmount())
                .startDate(budget.getStartDate())
                .endDate(endDate)
                .spentAmount(spent)
                .remainingAmount(remaining)
                .percentUsed(percentUsed)
                .alertTriggered(alertTriggered)
                .alertThresholdPercent(budget.getAlertThresholdPercent())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }
}
