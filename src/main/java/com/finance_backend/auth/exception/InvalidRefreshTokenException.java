package com.finance_backend.auth.exception;

import com.finance_backend.common.exception.UnauthorizedException;

public class InvalidRefreshTokenException extends UnauthorizedException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public static InvalidRefreshTokenException create() {
        return new InvalidRefreshTokenException("Refresh token is invalid, expired, or has been revoked");
    }
}
