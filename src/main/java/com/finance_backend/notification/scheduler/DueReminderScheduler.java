package com.finance_backend.notification.scheduler;

import com.finance_backend.notification.entity.NotificationType;
import com.finance_backend.notification.service.NotificationService;
import com.finance_backend.recurring.entity.RecurringTransaction;
import com.finance_backend.recurring.repository.RecurringTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

/**
 * Sends a reminder a few days before a recurring payment or income is due.
 * This only sends a notification; the actual transaction is created on the due date.
 */
@Slf4j
@Component
public class DueReminderScheduler {

    private static final int REMINDER_DAYS_BEFORE = 3;

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final NotificationService notificationService;

    public DueReminderScheduler(RecurringTransactionRepository recurringTransactionRepository,
                                NotificationService notificationService) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.notificationService = notificationService;
    }

    /** Runs daily at 09:00. */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendUpcomingDueReminders() {
        LocalDate reminderTargetDate = LocalDate.now().plusDays(REMINDER_DAYS_BEFORE);
        List<RecurringTransaction> upcoming =
                recurringTransactionRepository.findByIsActiveTrueAndNextDueDate(reminderTargetDate);

        for (RecurringTransaction rt : upcoming) {
            notificationService.createNotification(
                    rt.getUserId(),
                    NotificationType.DUE_REMINDER,
                    "Upcoming: " + rt.getTitle(),
                    String.format("Your %s (Rs.%s) is due on %s.", rt.getTitle(), rt.getAmount(), rt.getNextDueDate()),
                    "RECURRING_TRANSACTION",
                    rt.getId());
        }

        if (!upcoming.isEmpty()) {
            log.info("Sent {} due-soon reminder(s) for {}", upcoming.size(), reminderTargetDate);
        }
    }
}
