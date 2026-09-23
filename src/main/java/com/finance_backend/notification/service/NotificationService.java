package com.finance_backend.notification.service;

import com.finance_backend.notification.dto.NotificationResponse;
import com.finance_backend.notification.entity.NotificationType;

import java.util.List;

public interface NotificationService {

    /**
     * Used internally by other modules to create notifications.
     * It is not available through a public API, and related entity details are optional.
     */
    NotificationResponse createNotification(Long userId, NotificationType type, String title, String message,
                                            String relatedEntityType, Long relatedEntityId);

    List<NotificationResponse> getNotificationsByUserId(Long userId);

    List<NotificationResponse> getUnreadNotifications(Long userId);

    long getUnreadCount(Long userId);

    NotificationResponse markAsRead(Long id);

    void markAllAsRead(Long userId);

    void deleteNotification(Long id);
}
