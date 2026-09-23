package com.finance_backend.auth.security;

import com.finance_backend.common.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceOwnershipGuardTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void permitsTheOwnerButBlocksAnotherSignedInUser() {
        // Arrange
        ResourceOwnershipGuard guard = new ResourceOwnershipGuard(true);
        signIn(10L, Set.of("USER"));

        // Act + Assert
        assertThatCode(() -> guard.check(10L, "expense", 5L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.check(11L, "expense", 5L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own data");
    }

    @Test
    void permitsAdministratorsAndSkipsChecksWhenEnforcementIsDisabled() {
        // Arrange
        ResourceOwnershipGuard enforcedGuard = new ResourceOwnershipGuard(true);
        signIn(10L, Set.of("ADMIN"));

        // Act + Assert
        assertThatCode(() -> enforcedGuard.check(11L, "expense", 5L)).doesNotThrowAnyException();

        ResourceOwnershipGuard developmentGuard = new ResourceOwnershipGuard(false);
        signIn(10L, Set.of("USER"));
        assertThatCode(() -> developmentGuard.check(11L, "expense", 5L)).doesNotThrowAnyException();
    }

    @Test
    void blocksNonAdminsFromChangingSharedResources() {
        // Arrange
        ResourceOwnershipGuard guard = new ResourceOwnershipGuard(true);
        signIn(10L, Set.of("USER"));

        // Act + Assert
        assertThatThrownBy(() -> guard.requireAdmin("category", 5L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("administrator");
    }

    private void signIn(Long userId, Set<String> roles) {
        AuthPrincipal principal = new AuthPrincipal(userId, "user@example.com", roles);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
