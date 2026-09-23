package com.finance_backend.profile.exception;

import com.finance_backend.common.exception.ConflictException;

public class DuplicateProfileException extends ConflictException {

    public DuplicateProfileException(Long userId) {
        super("A profile already exists for userId: " + userId);
    }
}

