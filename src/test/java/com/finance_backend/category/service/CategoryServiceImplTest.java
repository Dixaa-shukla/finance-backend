package com.finance_backend.category.service;

import com.finance_backend.auth.security.ResourceOwnershipGuard;
import com.finance_backend.category.dto.CategoryRequest;
import com.finance_backend.category.entity.Category;
import com.finance_backend.category.entity.CategoryType;
import com.finance_backend.category.exception.DuplicateCategoryException;
import com.finance_backend.category.repository.CategoryRepository;
import com.finance_backend.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ResourceOwnershipGuard ownershipGuard;
    @InjectMocks private CategoryServiceImpl categoryService;

    @Test
    void rejectsACustomCategoryThatDuplicatesADefaultCategory() {
        // Arrange
        CategoryRequest request = request("Food");
        when(categoryRepository.existsByNameIgnoreCaseAndTypeAndUserIdIsNull("Food", CategoryType.EXPENSE))
                .thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> categoryService.createCustomCategory(7L, request))
                .isInstanceOf(DuplicateCategoryException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createsACustomCategoryForThePathUser() {
        // Arrange
        CategoryRequest request = request("Travel");
        when(categoryRepository.existsByNameIgnoreCaseAndTypeAndUserIdIsNull("Travel", CategoryType.EXPENSE))
                .thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndTypeAndUserId("Travel", CategoryType.EXPENSE, 7L))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        categoryService.createCustomCategory(7L, request);

        // Assert
        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(7L);
        assertThat(saved.getValue().isDefault()).isFalse();
    }

    @Test
    void refusesDirectDeletionOfADefaultCategory() {
        // Arrange
        Category category = Category.builder().id(3L).isDefault(true).build();
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));

        // Act + Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(3L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("admin endpoint");
        verify(ownershipGuard, never()).check(any(), any(), any());
        verify(categoryRepository, never()).delete(any());
    }

    private CategoryRequest request(String name) {
        return CategoryRequest.builder().name(name).type(CategoryType.EXPENSE)
                .icon("tag").colorHex("#123456").build();
    }
}
