package com.finance_backend.ai.expenseCategorization.repository;

import com.finance_backend.ai.expenseCategorization.entity.MerchantCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MerchantCategoryMappingRepository extends JpaRepository<MerchantCategoryMapping, Long> {

    Optional<MerchantCategoryMapping> findByUserIdAndMerchantKey(Long userId, String merchantKey);

    // ---------- Module 16 (Admin Panel) -- AI Usage Monitoring ----------
    // Each row is one merchant the AI has "learned" for a user, so these
    // counts measure smart-learning adoption (Module 12).

    /** How many merchants the AI has learned for one user. */
    long countByUserId(Long userId);

    /** How many distinct users have at least one learned merchant mapping. */
    @Query("SELECT COUNT(DISTINCT m.userId) FROM MerchantCategoryMapping m")
    long countDistinctUsers();
}

