package com.finance_backend.income.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * categoryId stores the category ID without directly linking to the Category entity.
 * The service checks that the category exists when income is added or updated.
 */
@Entity
@Table(name = "incomes", indexes = {
        @Index(name = "idx_incomes_user_id", columnList = "user_id"),
        @Index(name = "idx_incomes_income_date", columnList = "income_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK reference to the authenticated user (Module 1). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private IncomeSource source;

    /** Optional reference to a Category (Module 5) of type INCOME. */
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "income_date", nullable = false)
    private LocalDate incomeDate;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private boolean isRecurring = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}