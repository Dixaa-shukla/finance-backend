package com.finance_backend.ai.expenseCategorization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Body for POST /api/v1/ai/categorization/confirm -- called when the user
 * accepts (or corrects) a suggestion, teaching the mapping table.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorizationConfirmRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "merchant is required")
    @Size(max = 150, message = "merchant must not exceed 150 characters")
    private String merchant;

    @NotBlank(message = "confirmedCategory is required")
    @Size(max = 50, message = "confirmedCategory must not exceed 50 characters")
    private String confirmedCategory;
}
