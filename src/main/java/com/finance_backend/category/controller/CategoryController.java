package com.finance_backend.category.controller;

import com.finance_backend.category.dto.CategoryRequest;
import com.finance_backend.category.dto.CategoryResponse;
import com.finance_backend.category.entity.CategoryType;
import com.finance_backend.category.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/defaults")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createDefaultCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createDefaultCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * custom category owned by a specific user.
     * POST /api/v1/categories/custom/user/{userId}
     */
    @PostMapping("/custom/user/{userId}")
    public ResponseEntity<CategoryResponse> createCustomCategory(@PathVariable Long userId,
                                                                 @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCustomCategory(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch a single category by id.
     * GET /api/v1/categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    /**
     * List default categories, optionally filtered by type.
     * GET /api/v1/categories/defaults?type=EXPENSE
     */
    @GetMapping("/defaults")
    public ResponseEntity<List<CategoryResponse>> getDefaultCategories(
            @RequestParam(required = false) CategoryType type) {
        return ResponseEntity.ok(categoryService.getDefaultCategories(type));
    }

    /**
     * List all categories visible to a user (defaults + their own custom ones).
     * GET /api/v1/categories/user/{userId}?type=EXPENSE
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CategoryResponse>> getCategoriesForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) CategoryType type) {
        return ResponseEntity.ok(categoryService.getCategoriesForUser(userId, type));
    }

    /**
     * Update a category's name/type/icon/color.
     * PUT /api/v1/categories/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
                                                           @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    /**
     * Delete a custom category. Rejects deletion of default categories (400).
     * DELETE /api/v1/categories/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Force-delete any category, including defaults.
     * DELETE /api/v1/categories/admin/{id}
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategoryAsAdmin(@PathVariable Long id) {
        categoryService.deleteCategoryAsAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
