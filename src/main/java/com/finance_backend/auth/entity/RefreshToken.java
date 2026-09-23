package com.finance_backend.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "refresh_tokens_token",
                        columnNames = "token"
                )
        },
        indexes = {
                @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Random UUID handed to the client. Unique + indexed because every /refresh looks it up. */
    @Column(
            name = "token",
            nullable = false,
            unique = true,
            length = 64
    )
    private String token;

    /** The account this token authenticates. Logical reference to User.id. */
    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Column(
            name = "revoked",
            nullable = false
    )
    @Builder.Default
    private boolean revoked = false;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /** Named hasExpired, not isExpired, so it is never mistaken for a persisted column. */
    public boolean hasExpired() {
        return expiresAt == null || expiresAt.isBefore(LocalDateTime.now());
    }

    /** Usable exactly when it is neither revoked nor expired. */
    public boolean isUsable() {
        return !revoked && !hasExpired();
    }
}
