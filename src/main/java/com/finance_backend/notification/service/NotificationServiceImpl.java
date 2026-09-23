package com.finance_backend.notification.service;

import com.finance_backend.notification.dto.NotificationResponse;
import com.finance_backend.notification.entity.Notification;
import com.finance_backend.notification.entity.NotificationType;
import com.finance_backend.notification.exception.NotificationNotFoundException;
import com.finance_backend.notification.repository.NotificationRepository;
import com.finance_backend.auth.security.ResourceOwnershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;
    private final ResourceOwnershipGuard ownershipGuard;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   EmailNotificationService emailNotificationService,
                                   ResourceOwnershipGuard ownershipGuard) {
        this.notificationRepository = notificationRepository;
        this.emailNotificationService = emailNotificationService;
        this.ownershipGuard = ownershipGuard;
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(Long userId, NotificationType type, String title, String message,
                                                   String relatedEntityType, Long relatedEntityId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // In-app notification (above) always succeeds independently of this.
        // Email is a secondary channel that currently no-ops until Module 1
        // provides a real email address -- see EmailNotificationServiceImpl.
        emailNotificationService.sendIfPossible(userId, title, message);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        ownershipGuard.check(notification.getUserId(), "notification", id);
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void deleteNotification(Long id) {
        // findById, not existsById: the row has to be loaded to know who owns it.
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        ownershipGuard.check(notification.getUserId(), "notification", id);
        notificationRepository.delete(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
