package com.finance_backend.recurring.repository;

import com.finance_backend.recurring.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserId(Long userId);

    /** Used by the scheduler to find all recurring transactions that are due today or overdue. */
    List<RecurringTransaction> findByIsActiveTrueAndNextDueDateLessThanEqual(LocalDate date);

    /**
     * Module 14: Finds recurring transactions due on a specific future date.
     * This helps send one reminder before each payment or income is due.
     */
    List<RecurringTransaction> findByIsActiveTrueAndNextDueDate(LocalDate date);
}
