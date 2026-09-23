package com.finance_backend.investmentTracker.repository;

import com.finance_backend.investmentTracker.entity.Investment;
import com.finance_backend.investmentTracker.entity.InvestmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    List<Investment> findByUserId(Long userId);

    List<Investment> findByUserIdAndType(Long userId, InvestmentType type);
}
