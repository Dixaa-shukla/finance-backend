package com.finance_backend.common.exception;

/**
 * Base exception for failures when calling external services, such as AI, OCR,
 * or email APIs, which are returned as HTTP 502 because the external service failed.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
