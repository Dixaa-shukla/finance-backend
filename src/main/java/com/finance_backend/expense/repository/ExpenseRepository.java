package com.finance_backend.expense.repository;

import com.finance_backend.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    List<Expense> findByUserIdOrderByExpenseDateDesc(Long userId);

    /**
     * Module 6: Calculates a user's total spending across all categories for a selected date range.
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.userId = :userId AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserAndDateRange(@Param("userId") Long userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * Module 6: Calculates a user's spending for one category within a selected date range.
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.userId = :userId AND e.expenseDate BETWEEN :startDate AND :endDate " +
            "AND LOWER(e.category) = LOWER(:category)")
    BigDecimal sumAmountByUserAndDateRangeAndCategory(@Param("userId") Long userId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate,
                                                      @Param("category") String category);

    /**
     * Module 13: Finds users who have expense data for the monthly report.
     * Later, when the User table is added, this should use active users instead.
     */
    @Query("SELECT DISTINCT e.userId FROM Expense e")
    List<Long> findDistinctUserIds();

    /**
     * Module 8: Uses MySQL Full-Text Search to find expenses more accurately.
     * It works alongside the existing LIKE search and needs a FULLTEXT index
     * on merchant and notes, which must be added manually in the database.
     */
    @Query(value = "SELECT * FROM expenses WHERE user_id = :userId " +
            "AND MATCH(merchant, notes) AGAINST(:searchText IN NATURAL LANGUAGE MODE)",
            nativeQuery = true)
    List<Expense> fullTextSearch(@Param("userId") Long userId, @Param("searchText") String searchText);

    /**
     * Module 16: Counts how many expenses each user has for the admin user list.
     * This is faster than loading all of a user's expenses.
     */
    long countByUserId(Long userId);

    /**
     * Module 16: Calculates the total spending of all users on the platform.
     * Returns 0 instead of null when there are no expenses.
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
    BigDecimal sumAllAmounts();
}