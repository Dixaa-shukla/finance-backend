package com.finance_backend.auth.exception;

import com.finance_backend.common.exception.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException forId(Long id) {
        return new UserNotFoundException("User not found with id: " + id);
    }

    public static UserNotFoundException forEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }
}
