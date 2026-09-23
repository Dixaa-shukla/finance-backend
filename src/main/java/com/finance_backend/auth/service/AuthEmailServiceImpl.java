package com.finance_backend.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class AuthEmailServiceImpl implements AuthEmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String passwordResetBaseUrl;

    public AuthEmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${finance.auth.email.from:no-reply@finance-backend.local}") String fromAddress,
            @Value("${finance.auth.email.password-reset-base-url:http://localhost:5173/reset-password}")
            String passwordResetBaseUrl) {

        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.passwordResetBaseUrl = passwordResetBaseUrl;
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        String link = passwordResetBaseUrl + "?token=" + token;

        log.info("[AUTH-LINK] Password reset for {} -> {}", email, link);

        send(email,
                "Reset your password",
                """
                We received a request to reset the password on your Personal
                Finance Portal account.

                Open this link to choose a new password:

                %s

                The link expires in 60 minutes and can only be used once.
                If you did not request this, no action is needed -- your current
                password still works.
                """.formatted(link));
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            log.info("Sent '{}' email to {}", subject, to);

        } catch (Exception e) {
            log.warn("Could not send '{}' email to {} -- use the [AUTH-LINK] line above instead. Reason: {}",
                    subject, to, e.getMessage());
        }
    }
}
