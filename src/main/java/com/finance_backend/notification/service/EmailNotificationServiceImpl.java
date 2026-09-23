package com.finance_backend.notification.service;

import com.finance_backend.auth.entity.User;
import com.finance_backend.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Uses JavaMail to send emails.
 * After Module 1, it gets real user email addresses, so all notification emails can be sent.
 */
@Slf4j
@Service
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    public EmailNotificationServiceImpl(JavaMailSender mailSender,
                                        UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    @Override
    public void sendIfPossible(Long userId, String subject, String body) {
        Optional<String> email = getEmailForUser(userId);

        if (email.isEmpty()) {
            log.debug("Skipping email for userId={} -- no account with that id in the users table", userId);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email.get());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent notification email to userId={}", userId);
        } catch (Exception e) {
            // Email is a nice-to-have secondary channel -- a send failure
            // must never break the in-app notification that already succeeded.
            log.warn("Failed to send notification email for userId={}", userId, e);
        }
    }

    /**
     * Module 1 now stores each user's email address, so this method looks it up.
     * If the user is not found, it returns empty and the email is simply skipped.
     */
    private Optional<String> getEmailForUser(Long userId) {
        return userRepository.findById(userId).map(User::getEmail);
    }
}