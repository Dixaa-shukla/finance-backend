package com.finance_backend.auth.entity;

public enum AuthProvider {

    /** Registered through POST /api/v1/auth/register with a password. */
    LOCAL,
    GOOGLE
}
