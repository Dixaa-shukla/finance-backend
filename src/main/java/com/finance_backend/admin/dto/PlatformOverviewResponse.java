package com.finance_backend.admin.dto;

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
public class PlatformOverviewResponse {

    private long totalUsers;
    private long totalExpenses;
    private long totalIncomes;
    private long totalInvestments;

    private long totalCategories;
    /** System-wide categories (userId IS NULL), visible to every user. */
    private long defaultCategories;
    /** totalCategories - defaultCategories. */
    private long customCategories;
}
