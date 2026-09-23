package com.finance_backend.recurring.schedular;

import com.finance_backend.recurring.service.RecurringTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs every night and creates Expense/Income records for all recurring transactions that are due.
 */
@Slf4j
@Component
public class RecurringTransactionScheduler {

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionScheduler(RecurringTransactionService recurringTransactionService) {
        this.recurringTransactionService = recurringTransactionService;
    }

    /** Runs daily at 01:00, ahead of most people checking their app in the morning. */
    @Scheduled(cron = "0 0 1 * * *")
    public void processDueRecurringTransactions() {
        int generated = recurringTransactionService.processDueRecurringTransactions();
        if (generated > 0) {
            log.info("Recurring transaction sweep generated {} transaction(s).", generated);
        }
    }
}
