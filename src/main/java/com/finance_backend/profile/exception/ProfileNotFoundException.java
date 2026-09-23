package com.finance_backend.profile.exception;

import com.finance_backend.common.exception.ResourceNotFoundException;

public class ProfileNotFoundException extends ResourceNotFoundException {

    public ProfileNotFoundException(String message) {
        super(message);
    }

    public static ProfileNotFoundException forId(Long id) {
        return new ProfileNotFoundException("Profile not found with id: " + id);
    }

    public static ProfileNotFoundException forUserId(Long userId) {
        return new ProfileNotFoundException("Profile not found for userId: " + userId);
    }
}