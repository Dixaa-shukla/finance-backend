package com.finance_backend.notification.exception;

import com.finance_backend.common.exception.ResourceNotFoundException;

public class NotificationNotFoundException extends ResourceNotFoundException {

    public NotificationNotFoundException(Long id) {
        super("Notification not found with id: " + id);
    }
}
