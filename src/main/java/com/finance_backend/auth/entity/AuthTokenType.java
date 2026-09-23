package com.finance_backend.auth.entity;

public enum AuthTokenType {

    /** Sent by /forgot-password; consumed by POST /api/v1/auth/reset-password. */
    PASSWORD_RESET
}
