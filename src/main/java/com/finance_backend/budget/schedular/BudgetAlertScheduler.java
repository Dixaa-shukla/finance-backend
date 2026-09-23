package com.finance_backend.budget.schedular;

import com.finance_backend.budget.dto.BudgetResponse;
import com.finance_backend.budget.entity.Budget;
import com.finance_backend.budget.repository.BudgetRepository;
import com.finance_backend.budget.service.BudgetService;
import com.finance_backend.notification.entity.NotificationType;
import com.finance_backend.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BudgetAlertScheduler {

    private final BudgetRepository budgetRepository;
    private final BudgetService budgetService;
    private final NotificationService notificationService;

    public BudgetAlertScheduler(BudgetRepository budgetRepository,
                                BudgetService budgetService,
                                NotificationService notificationService) {
        this.budgetRepository = budgetRepository;
        this.budgetService = budgetService;
        this.notificationService = notificationService;
    }

    /** Runs every day at 08:00 server time. Cron: sec min hour day month weekday. */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkBudgetAlerts() {
        Set<Long> userIds = budgetRepository.findAll()
                .stream()
                .map(Budget::getUserId)
                .collect(Collectors.toSet());

        int totalAlerts = 0;
        for (Long userId : userIds) {
            List<BudgetResponse> triggered = budgetService.getTriggeredAlertsForUser(userId);
            for (BudgetResponse budget : triggered) {
                String categoryLabel = budget.getCategoryName() != null ? budget.getCategoryName() : "Overall";

                log.warn("Budget alert -- userId={}, budgetId={}, category={}, {}% used ({} of {})",
                        userId, budget.getId(), categoryLabel,
                        String.format("%.1f", budget.getPercentUsed()),
                        budget.getSpentAmount(), budget.getAmount());

                notificationService.createNotification(
                        userId,
                        NotificationType.BUDGET_ALERT,
                        categoryLabel + " budget alert",
                        String.format("You've used %.1f%% of your %s budget (Rs.%s of Rs.%s).",
                                budget.getPercentUsed(), categoryLabel, budget.getSpentAmount(), budget.getAmount()),
                        "BUDGET",
                        budget.getId());

                totalAlerts++;
            }
        }
        log.info("Budget alert sweep complete -- {} user(s) checked, {} alert(s) triggered", userIds.size(), totalAlerts);
    }
}
