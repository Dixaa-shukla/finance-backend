package com.finance_backend.ai.expenseCategorization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorizationRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    /** Raw expense description or merchant text, e.g. "Pizza from Domino's". */
    @NotBlank(message = "description is required")
    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;
}
