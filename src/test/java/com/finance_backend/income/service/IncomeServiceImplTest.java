package com.finance_backend.income.service;

import com.finance_backend.auth.security.ResourceOwnershipGuard;
import com.finance_backend.category.entity.Category;
import com.finance_backend.category.entity.CategoryType;
import com.finance_backend.category.repository.CategoryRepository;
import com.finance_backend.common.exception.BadRequestException;
import com.finance_backend.income.dto.IncomeRequest;
import com.finance_backend.income.dto.IncomeResponse;
import com.finance_backend.income.entity.Income;
import com.finance_backend.income.entity.IncomeSource;
import com.finance_backend.income.exception.IncomeNotFoundException;
import com.finance_backend.income.repository.IncomeRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncomeServiceImplTest {
    @Mock private IncomeRepository incomeRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ResourceOwnershipGuard ownershipGuard;
    @InjectMocks private IncomeServiceImpl incomeService;

    private IncomeRequest request() {
        return IncomeRequest.builder().userId(10L).amount(new BigDecimal("5000"))
                .source(IncomeSource.SALARY).categoryId(2L).incomeDate(LocalDate.now()).build();
    }

    @Test
    void createsIncomeForAnIncomeCategory() {
        // Arrange
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(Category.builder().id(2L).name("Salary").type(CategoryType.INCOME).build()));
        when(incomeRepository.save(any(Income.class))).thenAnswer(i -> { Income income = i.getArgument(0); income.setId(1L); return income; });

        // Act
        IncomeResponse result = incomeService.createIncome(request());

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCategoryName()).isEqualTo("Salary");
    }

    @Test
    void rejectsAnExpenseCategory() {
        // Arrange
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(Category.builder().id(2L).type(CategoryType.EXPENSE).build()));

        // Act / Assert
        assertThatThrownBy(() -> incomeService.createIncome(request())).isInstanceOf(BadRequestException.class);
        verify(incomeRepository, never()).save(any());
    }

    @Test
    void refusesToDeleteAnIncomeThatDoesNotExist() {
        // Arrange
        when(incomeRepository.findById(99L)).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> incomeService.deleteIncome(99L)).isInstanceOf(IncomeNotFoundException.class);
        verify(incomeRepository, never()).delete(any(Income.class));
    }
}
