package com.finance_backend.income.dto;

import com.finance_backend.income.entity.IncomeSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeResponse {

    private Long id;
    private Long userId;
    private BigDecimal amount;
    private IncomeSource source;
    private Long categoryId;
    /** resolved via CategoryRepository at read time. */
    private String categoryName;
    private LocalDate incomeDate;
    private String notes;
    private boolean isRecurring;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
