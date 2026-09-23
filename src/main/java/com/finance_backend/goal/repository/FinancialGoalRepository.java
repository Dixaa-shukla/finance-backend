package com.finance_backend.goal.repository;

import com.finance_backend.goal.entity.FinancialGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Long> {

    List<FinancialGoal> findByUserId(Long userId);
}
