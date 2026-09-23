package com.finance_backend.investmentTracker.dto;

import com.finance_backend.investmentTracker.entity.InvestmentType;
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
public class InvestmentRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "type is required")
    private InvestmentType type;

    @NotBlank(message = "name is required")
    @Size(max = 150, message = "name must not exceed 150 characters")
    private String name;

    @NotNull(message = "investedAmount is required")
    @DecimalMin(value = "0.01", message = "investedAmount must be greater than 0")
    private BigDecimal investedAmount;

    /** Optional; if not provided, it uses the invested amount, meaning there is no profit or loss yet. */
    @DecimalMin(value = "0.0", message = "currentValue cannot be negative")
    private BigDecimal currentValue;

    @DecimalMin(value = "0.0", message = "quantity cannot be negative")
    private BigDecimal quantity;

    @NotNull(message = "purchaseDate is required")
    @PastOrPresent(message = "purchaseDate cannot be in the future")
    private LocalDate purchaseDate;

    private LocalDate maturityDate;

    @DecimalMin(value = "0.0", message = "interestRate cannot be negative")
    @DecimalMax(value = "100.0", message = "interestRate cannot exceed 100")
    private BigDecimal interestRate;

    @Size(max = 500, message = "notes must not exceed 500 characters")
    private String notes;
}
