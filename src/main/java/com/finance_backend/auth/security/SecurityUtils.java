package com.finance_backend.auth.security;

import com.finance_backend.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
        // static utility
    }

    public static Optional<AuthPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Optional<Long> currentUserId() {
        return currentPrincipal().map(AuthPrincipal::userId);
    }

    public static AuthPrincipal currentPrincipalOrThrow() {
        return currentPrincipal().orElseThrow(() ->
                new UnauthorizedException("Authentication required -- send a valid Bearer access token"));
    }

    public static Long currentUserIdOrThrow() {
        return currentPrincipalOrThrow().userId();
    }

    public static boolean isCurrentUserAdmin() {
        return currentPrincipal().map(AuthPrincipal::hasAdminRole).orElse(false);
    }
}
