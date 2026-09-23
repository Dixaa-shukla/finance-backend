package com.finance_backend.recurring.entity;

import com.finance_backend.expense.entity.PaymentMethod;
import com.finance_backend.income.entity.IncomeSource;
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

@Entity
@Table(name = "recurring_transactions", indexes = {
        @Index(name = "idx_recurring_user_id", columnList = "user_id"),
        @Index(name = "idx_recurring_next_due_date", columnList = "next_due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK reference to the authenticated user (Module 1). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private RecurringTransactionType type;

    /** e.g. "Home Rent", "Car EMI", "Netflix Subscription". */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Required when type = EXPENSE; mirrors Expense.category (plain string). Ignored for INCOME. */
    @Column(name = "category", length = 50)
    private String category;

    /** Required when type = INCOME. Ignored for EXPENSE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "income_source", length = 20)
    private IncomeSource incomeSource;

    /** Optional; only meaningful when type = EXPENSE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 10)
    private RecurringFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** The next date this rule should fire. Advances after each generation. */
    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    /** Null = runs indefinitely. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** User can pause without deleting; also auto-set false once endDate is passed. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
