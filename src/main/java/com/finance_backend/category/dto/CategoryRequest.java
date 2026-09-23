package com.finance_backend.category.dto;

import com.finance_backend.category.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shared request body for creating/updating both default and custom
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank(message = "name is required")
    @Size(max = 50, message = "name must not exceed 50 characters")
    private String name;

    @NotNull(message = "type is required")
    private CategoryType type;

    @Size(max = 50, message = "icon must not exceed 50 characters")
    private String icon;

    @Pattern(regexp = "^$|^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",  message = "colorHex must be a valid hex color, e.g. #FF5733")
    private String colorHex;
}
