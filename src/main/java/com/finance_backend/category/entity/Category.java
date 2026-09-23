package com.finance_backend.category.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A single table serves both default (system-wide) and custom (per-user)
 * categories:
 *   - userId == null  -> default category, visible to every user, managed by admins
 *   - userId != null  -> custom category, owned and visible only to that user
 */
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_user_id", columnList = "user_id"),
        @Index(name = "idx_categories_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private CategoryType type;

    /** Optional icon identifier, e.g. an emoji or a frontend icon-library key. */
    @Column(name = "icon", length = 50)
    private String icon;

    /** Optional hex color for UI */
    @Column(name = "color_hex", length = 7)
    private String colorHex;

    /** true when userId is null (system default); kept as an explicit flag for fast filtering. */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    /** Null for default categories. Set for user-owned custom categories. */
    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
