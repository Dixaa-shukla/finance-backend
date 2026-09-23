package com.finance_backend.auth.entity;

public enum Role {
    USER,
    ADMIN;

    /** The Spring Security authority string, e.g. ADMIN -> "ROLE_ADMIN". */
    public String asAuthority() {
        return "ROLE_" + name();
    }
}
