package com.finance_backend.expense.entity;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Module 3: Expense Management.
 * Stores spending records; AI can update the category, and Receipt/OCR can add the receipt URL.
 */
@Entity
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expenses_user_id", columnList = "user_id"),
        @Index(name = "idx_expenses_expense_date", columnList = "expense_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK reference to the authenticated user (Module 1). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * Plain string for now. Once Module 5 (Category Management) exists,
     * replace with a `categoryId` FK to the Category entity.
     */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "merchant", length = 150)
    private String merchant;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "notes", length = 500)
    private String notes;

    /** Populated after upload via Module 17 (File & Document Management). */
    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    /** Optional free-text location, e.g. "Domino's, Gomti Nagar". */
    @Column(name = "location", length = 150)
    private String location;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_tags", joinColumns = @JoinColumn(name = "expense_id"))
    @Column(name = "tag", length = 30)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
