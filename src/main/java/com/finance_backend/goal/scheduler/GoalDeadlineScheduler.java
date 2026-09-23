package com.finance_backend.goal.scheduler;

import com.finance_backend.goal.entity.FinancialGoal;
import com.finance_backend.goal.repository.FinancialGoalRepository;
import com.finance_backend.notification.entity.NotificationType;
import com.finance_backend.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Runs daily to find goals that are close to their deadline or already expired.
 * It also creates in-app notifications to alert the user.
 */
@Slf4j
@Component
public class GoalDeadlineScheduler {

    private static final int DEADLINE_WARNING_DAYS = 7;

    private final FinancialGoalRepository goalRepository;
    private final NotificationService notificationService;

    public GoalDeadlineScheduler(FinancialGoalRepository goalRepository,
                                 NotificationService notificationService) {
        this.goalRepository = goalRepository;
        this.notificationService = notificationService;
    }

    /** Runs daily at 08:00. */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkGoalDeadlines() {
        LocalDate today = LocalDate.now();
        List<FinancialGoal> goals = goalRepository.findAll();

        for (FinancialGoal goal : goals) {
            boolean completed = goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0;
            if (completed) {
                continue;
            }

            long daysRemaining = ChronoUnit.DAYS.between(today, goal.getTargetDate());

            if (daysRemaining < 0) {
                log.warn("Goal '{}' (id={}, userId={}) is EXPIRED. Target: {}, Saved: {}",
                        goal.getTitle(), goal.getId(), goal.getUserId(),
                        goal.getTargetAmount(), goal.getCurrentAmount());

                notificationService.createNotification(
                        goal.getUserId(),
                        NotificationType.GOAL_DEADLINE,
                        "Goal deadline passed: " + goal.getTitle(),
                        String.format("Your goal '%s' passed its target date with Rs.%s of Rs.%s saved. "
                                        + "Consider updating the deadline or contributing more.",
                                goal.getTitle(), goal.getCurrentAmount(), goal.getTargetAmount()),
                        "GOAL",
                        goal.getId());

            } else if (daysRemaining <= DEADLINE_WARNING_DAYS) {
                BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());

                log.info("Goal '{}' (id={}, userId={}) deadline in {} day(s). Remaining: {}",
                        goal.getTitle(), goal.getId(), goal.getUserId(), daysRemaining, remaining);

                notificationService.createNotification(
                        goal.getUserId(),
                        NotificationType.GOAL_DEADLINE,
                        "Goal deadline approaching: " + goal.getTitle(),
                        String.format("Your goal '%s' is due in %d day(s). Rs.%s still needed.",
                                goal.getTitle(), daysRemaining, remaining),
                        "GOAL",
                        goal.getId());
            }
        }
    }
}