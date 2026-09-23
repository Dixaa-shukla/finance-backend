package com.finance_backend.investmentTracker.entity;

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
 * Module 10: Investment Tracker.
 * One entity stores all investment types, and unused fields are simply left empty.
 */
@Entity
@Table(name = "investments", indexes = {
        @Index(name = "idx_investments_user_id", columnList = "user_id"),
        @Index(name = "idx_investments_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK reference to the authenticated user (Module 1). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private InvestmentType type;

    /** e.g. "HDFC Flexicap Fund", "Reliance Industries", "SBI PPF Account", "Bitcoin". */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Total principal invested/contributed so far. */
    @Column(name = "invested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal investedAmount;

    /** Current market/account value -- manually updated by the user for now. */
    @Column(name = "current_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentValue;

    /** Units/shares/grams held. Optional -- not meaningful for FD/PPF/EPF/NPS. */
    @Column(name = "quantity", precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    /** Optional. Relevant for FD, PPF, EPF, NPS, Bonds. */
    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    /** Optional annual interest rate as a percentage, e.g. 7.1 for PPF. */
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
