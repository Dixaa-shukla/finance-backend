package com.finance_backend.ai.usage.service;

import com.finance_backend.ai.usage.entity.AiModule;

/**
 * Records what happened on each outbound AI provider call.
 *
 * Deliberately just two methods, both void and both returning nothing useful:

 * usedFallback refers to WHICH ChatClient bean served the attempt, not whether
 * the overall request eventually succeeded. One user action that fails over
 * produces two calls here: recordFailure(.., false, ..) then
 * recordSuccess(.., true, ..).
 */
public interface AiUsageTrackingService {

    /**
     * @param module       the AI feature that made the call
     * @param userId       may be null for scheduler-driven calls
     * @param usedFallback true if the secondary provider served this attempt
     * @param latencyMs    duration of this attempt alone
     */
    void recordSuccess(AiModule module, Long userId, boolean usedFallback, long latencyMs);

    /**
     * @param errorMessage provider error text; truncated by the implementation
     */
    void recordFailure(AiModule module, Long userId, boolean usedFallback, long latencyMs, String errorMessage);
}
