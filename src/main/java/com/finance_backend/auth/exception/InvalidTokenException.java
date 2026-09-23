package com.finance_backend.auth.exception;

import com.finance_backend.common.exception.BadRequestException;

public class InvalidTokenException extends BadRequestException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public static InvalidTokenException forPasswordReset() {
        return new InvalidTokenException(
                "Password reset link is invalid, expired, or has already been used");
    }
}
