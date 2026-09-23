package com.finance_backend.auth.security;

import com.finance_backend.auth.entity.Role;
import com.finance_backend.auth.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-thirty-two-bytes";

    @Test
    void generatesAndParsesAnAccessTokenWithIdentityAndRoles() {
        // Arrange
        JwtService service = new JwtService(SECRET, "test-issuer", 60);
        User user = User.builder().id(42L).email("user@example.com")
                .roles(Set.of(Role.USER, Role.ADMIN)).build();

        // Act
        AuthPrincipal principal = service.parse(service.generateAccessToken(user)).orElseThrow();

        // Assert
        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.email()).isEqualTo("user@example.com");
        assertThat(principal.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void rejectsTamperedOrExpiredTokens() {
        // Arrange
        JwtService activeService = new JwtService(SECRET, "test-issuer", 60);
        JwtService expiredService = new JwtService(SECRET, "test-issuer", -1);
        User user = User.builder().id(42L).email("user@example.com").roles(Set.of(Role.USER)).build();

        // Act + Assert
        assertThat(activeService.parse(activeService.generateAccessToken(user) + "tampered")).isEmpty();
        assertThat(expiredService.parse(expiredService.generateAccessToken(user))).isEmpty();
    }

    @Test
    void refusesAnUnsafeSigningSecret() {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> new JwtService("too-short", "test-issuer", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }
}
