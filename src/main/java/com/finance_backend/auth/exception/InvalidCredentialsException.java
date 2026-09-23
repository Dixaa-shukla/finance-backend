package com.finance_backend.auth.exception;

import com.finance_backend.common.exception.UnauthorizedException;

public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public static InvalidCredentialsException forLogin() {
        return new InvalidCredentialsException("Invalid email or password");
    }

    /** Wrong current password on POST /api/v1/auth/change-password. */
    public static InvalidCredentialsException forCurrentPassword() {
        return new InvalidCredentialsException("Current password is incorrect");
    }

    /** The account is disabled -- distinct because it is not a guessing attempt. */
    public static InvalidCredentialsException forDisabledAccount() {
        return new InvalidCredentialsException("This account has been disabled");
    }
}
