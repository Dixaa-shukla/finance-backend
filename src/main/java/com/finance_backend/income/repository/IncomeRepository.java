package com.finance_backend.income.repository;

import com.finance_backend.income.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long>, JpaSpecificationExecutor<Income> {

    List<Income> findByUserIdOrderByIncomeDateDesc(Long userId);

     // Module 16: Counts each user's income for the admin user list.

    long countByUserId(Long userId);

    /**
     * Module 16: Calculates the total income of all users on the platform.
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i")
    BigDecimal sumAllAmounts();
}