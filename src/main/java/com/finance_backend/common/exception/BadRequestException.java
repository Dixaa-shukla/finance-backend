package com.finance_backend.common.exception;

/**
 * Handles business-rule errors that are not simple field-validation errors,
 * such as invalid dates or trying to delete a budget with active alerts.
 */
public class BadRequestException extends RuntimeException {

        public BadRequestException(String message) {
            super(message);
        }
    }