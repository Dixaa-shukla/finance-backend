package com.finance_backend.goal.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "title is required")
    @Size(max = 100, message = "title must not exceed 100 characters")
    private String title;

    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;

    @NotNull(message = "targetAmount is required")
    @DecimalMin(value = "0.01", message = "targetAmount must be greater than 0")
    private BigDecimal targetAmount;

    @NotNull(message = "targetDate is required")
    @FutureOrPresent(message = "targetDate cannot be in the past")
    private LocalDate targetDate;
}
