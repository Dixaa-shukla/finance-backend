package com.finance_backend.category.dto;

import com.finance_backend.category.entity.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private CategoryType type;
    private String icon;
    private String colorHex;
    private boolean isDefault;
    /** Null for default categories. */
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}