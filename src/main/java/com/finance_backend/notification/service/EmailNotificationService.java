package com.finance_backend.notification.service;

public interface EmailNotificationService {

    /**
     * Sends an email only when the user's email address is available.
     */
    void sendIfPossible(Long userId, String subject, String body);
}