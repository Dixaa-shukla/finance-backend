package com.finance_backend.admin.controller;

import com.finance_backend.category.dto.CategoryRequest;
import com.finance_backend.category.dto.CategoryResponse;
import com.finance_backend.category.entity.CategoryType;
import com.finance_backend.category.service.CategoryService;
import com.finance_backend.common.dtos.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Create a system-wide default category (visible to every user).
     * POST /api/v1/admin/categories/defaults
     *
     * SECURED (Module 1): covered by the class-level @PreAuthorize("hasRole('ADMIN')").
     */
    @PostMapping("/defaults")
    public ResponseEntity<ApiResponse<CategoryResponse>> createDefaultCategory(
            @Valid @RequestBody CategoryRequest request) {

        CategoryResponse created = categoryService.createDefaultCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Default category created", created));
    }

    /**
     * List default categories, optionally filtered by type.
     * GET /api/v1/admin/categories/defaults?type=EXPENSE
     *
     * SECURED (Module 1): covered by the class-level @PreAuthorize("hasRole('ADMIN')").
     */
    @GetMapping("/defaults")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getDefaultCategories(
            @RequestParam(required = false) CategoryType type) {

        return ResponseEntity.ok(ApiResponse.success(categoryService.getDefaultCategories(type)));
    }

    /**
     * Update any category -- default or custom.
     * PUT /api/v1/admin/categories/{id}
     *
     * SECURED (Module 1): covered by the class-level @PreAuthorize("hasRole('ADMIN')").
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Category updated", categoryService.updateCategory(id, request)));
    }

    /**
     * Force-delete any category, including defaults (which the user-facing
     * endpoint refuses to touch).
     * DELETE /api/v1/admin/categories/{id}
     *
     * SECURED (Module 1): covered by the class-level @PreAuthorize("hasRole('ADMIN')").
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategoryAsAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
