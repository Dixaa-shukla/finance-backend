package com.finance_backend.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "users_email",
                        columnNames = "email"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(
            name = "email",
            nullable = false,
            unique = true,
            length = 150
    )
    private String email;
    @Column(
            name = "password",
            length = 100
    )
    private String password;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "user_roles_user_id_role",
                            columnNames = {"user_id", "role"}
                    )
            }
    )
    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    @Column(
            name = "enabled",
            nullable = false
    )
    @Builder.Default
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "provider",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;
    @Column(
            name = "provider_id",
            length = 128
    )
    private String providerId;

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

    /**
     * Named hasRole rather than isAdmin/getAdmin so Jackson can never pick it
     * up as a bean property -- entities are mapped to DTOs, never serialized.
     */
    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }
}
