package com.finance_backend.category.repository;

import com.finance_backend.category.entity.Category;
import com.finance_backend.category.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdIsNull();

    List<Category> findByUserIdIsNullAndType(CategoryType type);

    List<Category> findByUserId(Long userId);

    List<Category> findByUserIdAndType(Long userId, CategoryType type);

    boolean existsByNameIgnoreCaseAndTypeAndUserIdIsNull(String name, CategoryType type);

    boolean existsByNameIgnoreCaseAndTypeAndUserId(String name, CategoryType type, Long userId);

    /**
     * Added for Module 16 (Admin Panel): how many system-wide default
     * categories exist. Custom categories are derived as count() - this.
     */
    long countByUserIdIsNull();
}
