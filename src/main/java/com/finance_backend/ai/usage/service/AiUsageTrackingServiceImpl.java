package com.finance_backend.ai.usage.service;

import com.finance_backend.ai.usage.entity.AiModule;
import com.finance_backend.ai.usage.entity.AiProvider;
import com.finance_backend.ai.usage.entity.AiUsageLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiUsageTrackingServiceImpl implements AiUsageTrackingService {

    private static final Logger log = LoggerFactory.getLogger(AiUsageTrackingServiceImpl.class);

    /** Must match AiUsageLog.errorMessage's column length. */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    @Value("${finance.ai.primary-provider:gemini}")
    private String primaryProvider;

    private final AiUsageLogWriter aiUsageLogWriter;

    public AiUsageTrackingServiceImpl(AiUsageLogWriter aiUsageLogWriter) {
        this.aiUsageLogWriter = aiUsageLogWriter;
    }

    @Override
    public void recordSuccess(AiModule module, Long userId, boolean usedFallback, long latencyMs) {
        write(AiUsageLog.builder()
                .userId(userId)
                .module(module)
                .provider(resolveProvider(usedFallback))
                .usedFallback(usedFallback)
                .success(true)
                .latencyMs(latencyMs)
                .build());
    }

    @Override
    public void recordFailure(AiModule module, Long userId, boolean usedFallback, long latencyMs, String errorMessage) {
        write(AiUsageLog.builder()
                .userId(userId)
                .module(module)
                .provider(resolveProvider(usedFallback))
                .usedFallback(usedFallback)
                .success(false)
                .latencyMs(latencyMs)
                .errorMessage(truncate(errorMessage))
                .build());
    }

    /**
     * Telemetry must never break the feature it measures, so every failure is
     * swallowed with a warning. Note that a failed write is itself usually a
     * symptom of the database being unreachable, in which case the caller has
     * far bigger problems than a missing metric.
     */
    private void write(AiUsageLog entry) {
        try {
            aiUsageLogWriter.write(entry);
        } catch (Exception ex) {
            log.warn("Could not record AI usage for module={} (ignored): {}", entry.getModule(), ex.getMessage());
        }
    }

    /**
     * Which provider served the attempt. ChatClientConfig builds the fallback
     * client from whichever of the two providers is NOT primary, so the served
     * provider is Ollama exactly when "primary is Ollama" and "this was the
     * fallback attempt" disagree.
     */
    private AiProvider resolveProvider(boolean usedFallback) {
        boolean primaryIsOllama = "ollama".equalsIgnoreCase(primaryProvider);
        return usedFallback != primaryIsOllama ? AiProvider.OLLAMA : AiProvider.GEMINI;
    }

    private String truncate(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
