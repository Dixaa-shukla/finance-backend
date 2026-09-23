package com.finance_backend.common.exception;

/**
 * Base exception for errors where the user is logged in but does not have
 * permission to access the requested resource. Returns HTTP 403 Forbidden.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
