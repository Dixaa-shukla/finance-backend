package com.finance_backend.category.service;

import com.finance_backend.category.dto.CategoryRequest;
import com.finance_backend.category.dto.CategoryResponse;
import com.finance_backend.category.entity.Category;
import com.finance_backend.category.entity.CategoryType;
import com.finance_backend.category.exception.CategoryNotFoundException;
import com.finance_backend.category.exception.DuplicateCategoryException;
import com.finance_backend.category.repository.CategoryRepository;
import com.finance_backend.auth.security.ResourceOwnershipGuard;
import com.finance_backend.common.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ResourceOwnershipGuard ownershipGuard;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               ResourceOwnershipGuard ownershipGuard) {
        this.categoryRepository = categoryRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @Override
    @Transactional
    public CategoryResponse createDefaultCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCaseAndTypeAndUserIdIsNull(request.getName(), request.getType())) {
            throw new DuplicateCategoryException(request.getName(), request.getType());
        }
        Category category = toEntity(request, new Category());
        category.setUserId(null);
        category.setDefault(true);
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse createCustomCategory(Long userId, CategoryRequest request) {
        boolean duplicateInDefaults = categoryRepository
                .existsByNameIgnoreCaseAndTypeAndUserIdIsNull(request.getName(), request.getType());
        boolean duplicateInOwn = categoryRepository
                .existsByNameIgnoreCaseAndTypeAndUserId(request.getName(), request.getType(), userId);

        if (duplicateInDefaults || duplicateInOwn) {
            throw new DuplicateCategoryException(request.getName(), request.getType());
        }

        Category category = toEntity(request, new Category());
        category.setUserId(userId);
        category.setDefault(false);
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        /*
         * ⚠️ CATEGORY IS THE ONE MODULE WITH SHARED ROWS. A default category has
         * userId == null and every user is meant to read it, which check() allows
         * by design; only a CUSTOM category (userId set) is private.
         */
        ownershipGuard.check(category.getUserId(), "category", id);
        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getDefaultCategories(CategoryType type) {
        List<Category> categories = (type != null)
                ? categoryRepository.findByUserIdIsNullAndType(type)
                : categoryRepository.findByUserIdIsNull();

        return categories.stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(CategoryResponse::getName))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesForUser(Long userId, CategoryType type) {
        List<Category> defaults = (type != null)
                ? categoryRepository.findByUserIdIsNullAndType(type)
                : categoryRepository.findByUserIdIsNull();

        List<Category> custom = (type != null)
                ? categoryRepository.findByUserIdAndType(userId, type)
                : categoryRepository.findByUserId(userId);

        List<Category> combined = new ArrayList<>(defaults);
        combined.addAll(custom);

        return combined.stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(CategoryResponse::getName))
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        /*
         * Two different rules, because a Category can be either kind of row.
         * (This replaces the TODO that sat here before Module 1 existed.)
         *
         *   default (userId == null) -> shared by the whole platform, admin only
         *   custom  (userId set)     -> private, owner only
         *
         * Renaming a shared "Groceries" would change it for every user on the
         * platform, so it cannot be left open to whoever guesses its id.
         */
        if (existing.getUserId() == null) {
            ownershipGuard.requireAdmin("category", id);
        } else {
            ownershipGuard.check(existing.getUserId(), "category", id);
        }

        existing.setName(request.getName());
        existing.setType(request.getType());
        existing.setIcon(request.getIcon());
        existing.setColorHex(request.getColorHex());

        return toResponse(categoryRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        if (category.isDefault()) {
            throw new BadRequestException(
                    "Default categories cannot be deleted directly. Use the admin endpoint.");
        }
        // Past the isDefault guard this is always a custom row, so it has an owner.
        ownershipGuard.check(category.getUserId(), "category", id);
        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public void deleteCategoryAsAdmin(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteById(id);
    }

    // ---------- mapping helpers ----------

    private Category toEntity(CategoryRequest request, Category target) {
        target.setName(request.getName());
        target.setType(request.getType());
        target.setIcon(request.getIcon());
        target.setColorHex(request.getColorHex());
        return target;
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .icon(category.getIcon())
                .colorHex(category.getColorHex())
                .isDefault(category.isDefault())
                .userId(category.getUserId())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}