package com.finance_backend.auth.security;

import java.util.Set;

public record AuthPrincipal(Long userId, String email, Set<String> roles) {

    /** Named without an "is"/"get" prefix */
    public boolean hasAdminRole() {
        return roles != null && roles.contains("ADMIN");
    }
}
