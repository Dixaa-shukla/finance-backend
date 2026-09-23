package com.finance_backend.profile.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inbound payload for creating/updating a user profile.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "fullName is required")
    @Size(
            max = 100,
            message = "fullName must not exceed 100 characters"
    )
    private String fullName;

    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "phoneNumber must be a valid number"
    )
    private String phoneNumber;

    @Past(
            message = "dateOfBirth must be in the past"
    )
    private LocalDate dateOfBirth;

    @Size(
            max = 20,
            message = "gender must not exceed 20 characters"
    )
    private String gender;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "monthlySalary cannot be negative"
    )
    private BigDecimal monthlySalary;

    @NotBlank(
            message = "preferredCurrency is required"
    )
    @Size(
            min = 3,
            max = 3,
            message = "preferredCurrency must be a 3-letter ISO code, e.g. INR"
    )
    private String preferredCurrency;

    @Size(
            max = 255,
            message = "primaryFinancialGoal must not exceed 255 characters"
    )
    private String primaryFinancialGoal;

    @Size(
            max = 500
    )
    private String profilePictureUrl;
}