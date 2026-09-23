package com.finance_backend.common.exception;
/**
 * Base exception for "already exists" or conflicting-state errors across modules,
 * allowing one global handler to return HTTP 409 without separate handlers.
 */
public class ConflictException extends RuntimeException {

        public ConflictException(String message) {
            super(message);
        }
    }
