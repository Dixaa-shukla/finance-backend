package com.finance_backend.auth.exception;

import com.finance_backend.common.exception.ConflictException;

public class EmailAlreadyExistsException extends ConflictException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }

    public static EmailAlreadyExistsException forEmail(String email) {
        return new EmailAlreadyExistsException("An account already exists with email: " + email);
    }
}
