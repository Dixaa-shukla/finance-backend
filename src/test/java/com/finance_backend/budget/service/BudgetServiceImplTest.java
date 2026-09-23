package com.finance_backend.budget.service;

import com.finance_backend.auth.security.ResourceOwnershipGuard;
import com.finance_backend.budget.dto.BudgetRequest;
import com.finance_backend.budget.dto.BudgetResponse;
import com.finance_backend.budget.entity.Budget;
import com.finance_backend.budget.entity.BudgetPeriod;
import com.finance_backend.budget.repository.BudgetRepository;
import com.finance_backend.category.entity.Category;
import com.finance_backend.category.entity.CategoryType;
import com.finance_backend.category.repository.CategoryRepository;
import com.finance_backend.common.exception.BadRequestException;
import com.finance_backend.expense.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {
    @Mock private BudgetRepository budgetRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ResourceOwnershipGuard ownershipGuard;
    @InjectMocks private BudgetServiceImpl budgetService;

    private BudgetRequest request() {
        return BudgetRequest.builder().userId(10L).categoryId(3L).period(BudgetPeriod.MONTHLY)
                .amount(new BigDecimal("1000")).startDate(LocalDate.of(2026, 2, 1)).alertThresholdPercent(80).build();
    }

    @Test
    void calculatesMonthlyEndDateAndTriggersAlertAtThreshold() {
        // Arrange
        Category groceries = Category.builder().id(3L).name("Groceries").type(CategoryType.EXPENSE).build();
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(groceries));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> { Budget budget = i.getArgument(0); budget.setId(1L); return budget; });
        when(expenseRepository.sumAmountByUserAndDateRangeAndCategory(eq(10L), eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 2, 28)), eq("Groceries"))).thenReturn(new BigDecimal("800"));

        // Act
        BudgetResponse result = budgetService.createBudget(request());

        // Assert
        assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(result.getPercentUsed()).isEqualTo(80.0);
        assertThat(result.isAlertTriggered()).isTrue();
    }

    @Test
    void rejectsAnIncomeCategoryForABudget() {
        // Arrange
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(Category.builder().type(CategoryType.INCOME).build()));

        // Act / Assert
        assertThatThrownBy(() -> budgetService.createBudget(request())).isInstanceOf(BadRequestException.class);
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void checksOwnershipBeforeSavingAnUpdate() {
        // Arrange
        Budget existing = Budget.builder().id(1L).userId(44L).categoryId(3L).period(BudgetPeriod.MONTHLY).amount(BigDecimal.TEN).startDate(LocalDate.now()).alertThresholdPercent(80).build();
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(Category.builder().type(CategoryType.EXPENSE).build()));
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));
        when(expenseRepository.sumAmountByUserAndDateRangeAndCategory(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        // Act
        budgetService.updateBudget(1L, request());

        // Assert
        verify(ownershipGuard).check(44L, "budget", 1L);
    }
}
