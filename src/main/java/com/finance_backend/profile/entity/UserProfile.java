package com.finance_backend.profile.entity;

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
 * Stores the user's personal details, salary, currency, and profile picture.
 * userId connects this profile to the account created by Module 1.
 */
@Entity
@Table(
        name = "user_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "user_profiles_user_id",
                        columnNames = "user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK reference to the authenticated user (Module 1).
     * One profile per user.
     */
    @Column(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private Long userId;

    @Column(
            name = "full_name",
            nullable = false,
            length = 100
    )
    private String fullName;

    @Column(
            name = "phone_number",
            length = 15
    )
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(
            name = "gender",
            length = 20
    )
    private String gender;

    /**
     * Stores the user's monthly salary or income for AI budget recommendations.
     */
    @Column(
            name = "monthly_salary",
            precision = 15,
            scale = 2
    )
    private BigDecimal monthlySalary;

    /**
     * ISO 4217 currency code, e.g. INR.
     */
    @Column(
            name = "preferred_currency",
            nullable = false,
            length = 3
    )
    @Builder.Default
    private String preferredCurrency = "INR";

    /**
     * Short summary of the user's primary financial goal.
     */
    @Column(
            name = "primary_financial_goal",
            length = 255
    )
    private String primaryFinancialGoal;

    @Column(
            name = "profile_picture_url",
            length = 500
    )
    private String profilePictureUrl;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;
}