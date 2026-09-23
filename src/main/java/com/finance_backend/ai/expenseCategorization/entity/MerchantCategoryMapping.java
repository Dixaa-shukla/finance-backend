package com.finance_backend.ai.expenseCategorization.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "merchant_category_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_merchant_mapping_user_merchant", columnNames = {"user_id", "merchant_key"})
}, indexes = {
        @Index(name = "idx_merchant_mapping_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantCategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Normalized (lowercased, punctuation-stripped) merchant text used as the lookup key. */
    @Column(name = "merchant_key", nullable = false, length = 150)
    private String merchantKey;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /** How many times the user has confirmed this mapping -- not currently used for logic, but useful data. */
    @Column(name = "times_confirmed", nullable = false)
    @Builder.Default
    private int timesConfirmed = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}