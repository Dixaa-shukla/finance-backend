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
        name = "auth_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "auth_tokens_token",
                        columnNames = "token"
                )
        },
        indexes = {
                @Index(name = "idx_auth_tokens_user_id_type", columnList = "user_id, token_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Random UUID embedded in the emailed link. */
    @Column(
            name = "token",
            nullable = false,
            unique = true,
            length = 64
    )
    private String token;

    /** Logical reference to User.id -- see the note on {@link User}. */
    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "token_type",
            nullable = false,
            length = 30
    )
    private AuthTokenType tokenType;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    public boolean hasExpired() {
        return expiresAt == null || expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean hasBeenUsed() {
        return usedAt != null;
    }

    /** Redeemable exactly once, and only before it expires. */
    public boolean isUsable() {
        return !hasBeenUsed() && !hasExpired();
    }
}
