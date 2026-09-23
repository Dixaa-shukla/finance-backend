package com.finance_backend.ai.usage.service;

import com.finance_backend.ai.usage.entity.AiUsageLog;
import com.finance_backend.ai.usage.repository.AiUsageLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commits a single usage row in its OWN transaction.
 *
 * REQUIRES_NEW is essential rather than cosmetic. The services being measured
 * are themselves @Transactional , and a failure row written in the caller's transaction would be rolled
 * back by the very failure it exists to record.
 */
@Component
public class AiUsageLogWriter {

    private final AiUsageLogRepository aiUsageLogRepository;

    public AiUsageLogWriter(AiUsageLogRepository aiUsageLogRepository) {
        this.aiUsageLogRepository = aiUsageLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AiUsageLog entry) {
        aiUsageLogRepository.save(entry);
    }
}
