package com.finance_backend.ai.usage.repository;

import com.finance_backend.ai.usage.entity.AiModule;
import com.finance_backend.ai.usage.entity.AiProvider;
import com.finance_backend.ai.usage.entity.AiUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Everything here is a COUNT/AVG pushed down to the database -- the log table
 * grows with every AI call, so loading rows into memory to tally them would not
 * survive real usage. Only the paged log listing returns entities.
 */
@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    long countBySuccess(boolean success);

    long countByUsedFallback(boolean usedFallback);

    long countByProvider(AiProvider provider);

    long countByModule(AiModule module);

    long countByCreatedAtAfter(LocalDateTime cutoff);

    /** Newest first -- the admin log view is a troubleshooting tool. */
    Page<AiUsageLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Averaged over successful calls only: a failed attempt's latency measures
     * how fast the provider rejected us, which would distort the figure.
     * Returns null when no successful call has been recorded yet.
     */
    @Query("SELECT AVG(l.latencyMs) FROM AiUsageLog l WHERE l.success = true AND l.latencyMs IS NOT NULL")
    Double averageSuccessfulLatencyMs();

    @Query("SELECT COUNT(DISTINCT l.userId) FROM AiUsageLog l WHERE l.userId IS NOT NULL")
    long countDistinctUsers();
}
