package com.finance_backend.category.service;

import com.finance_backend.category.dto.CategoryRequest;
import com.finance_backend.category.dto.CategoryResponse;
import com.finance_backend.category.entity.CategoryType;

import java.util.List;

public interface CategoryService {

    /** Admin operation: system-wide default category (userId = null). */
    CategoryResponse createDefaultCategory(CategoryRequest request);

    /**custom category owned by the given user. */
    CategoryResponse createCustomCategory(Long userId, CategoryRequest request);

    CategoryResponse getCategoryById(Long id);

    List<CategoryResponse> getDefaultCategories(CategoryType type);

    /** Defaults + the user's own custom categories, merged and sorted by name. */
    List<CategoryResponse> getCategoriesForUser(Long userId, CategoryType type);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    /** Blocks deleting a default category; use deleteCategoryAsAdmin for that. */
    void deleteCategory(Long id);

    /** deletes regardless of ownership. */
    void deleteCategoryAsAdmin(Long id);
}
