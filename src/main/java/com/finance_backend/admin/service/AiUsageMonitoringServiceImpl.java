package com.finance_backend.admin.service;

import com.finance_backend.admin.dto.AiUsageLogResponse;
import com.finance_backend.admin.dto.AiUsageSummaryResponse;
import com.finance_backend.ai.expenseCategorization.repository.MerchantCategoryMappingRepository;
import com.finance_backend.ai.financialAssistant.entity.ChatMessageRole;
import com.finance_backend.ai.financialAssistant.repository.ChatHistoryRepository;
import com.finance_backend.ai.spendingAnalytics.repository.MonthlyReportRepository;
import com.finance_backend.ai.usage.entity.AiModule;
import com.finance_backend.ai.usage.entity.AiProvider;
import com.finance_backend.ai.usage.entity.AiUsageLog;
import com.finance_backend.ai.usage.repository.AiUsageLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiUsageMonitoringServiceImpl implements AiUsageMonitoringService {

    /** Window used for the "recent activity" metrics. */
    private static final int RECENT_ACTIVITY_DAYS = 7;

    private final ChatHistoryRepository chatHistoryRepository;
    private final MerchantCategoryMappingRepository merchantCategoryMappingRepository;
    private final MonthlyReportRepository monthlyReportRepository;
    private final AiUsageLogRepository aiUsageLogRepository;

    public AiUsageMonitoringServiceImpl(ChatHistoryRepository chatHistoryRepository,
                                        MerchantCategoryMappingRepository merchantCategoryMappingRepository,
                                        MonthlyReportRepository monthlyReportRepository,
                                        AiUsageLogRepository aiUsageLogRepository) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.merchantCategoryMappingRepository = merchantCategoryMappingRepository;
        this.monthlyReportRepository = monthlyReportRepository;
        this.aiUsageLogRepository = aiUsageLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AiUsageSummaryResponse getAiUsage() {
        long userMessages = chatHistoryRepository.countByRole(ChatMessageRole.USER);
        long aiResponses = chatHistoryRepository.countByRole(ChatMessageRole.ASSISTANT);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RECENT_ACTIVITY_DAYS);

        return AiUsageSummaryResponse.builder()
                // ---- adoption: derived from the feature tables ----
                .totalChatMessages(chatHistoryRepository.count())
                .totalUserMessages(userMessages)
                .totalAiResponses(aiResponses)
                .distinctChatbotUsers(chatHistoryRepository.countDistinctUsers())
                .chatMessagesLast7Days(chatHistoryRepository.countByCreatedAtAfter(cutoff))
                .totalLearnedMerchants(merchantCategoryMappingRepository.count())
                .distinctCategorizationUsers(merchantCategoryMappingRepository.countDistinctUsers())
                .totalMonthlyReports(monthlyReportRepository.count())
                // ---- telemetry: from ai_usage_log ----
                .totalAiCalls(aiUsageLogRepository.count())
                .successfulAiCalls(aiUsageLogRepository.countBySuccess(true))
                .failedAiCalls(aiUsageLogRepository.countBySuccess(false))
                .fallbackAiCalls(aiUsageLogRepository.countByUsedFallback(true))
                .geminiCalls(aiUsageLogRepository.countByProvider(AiProvider.GEMINI))
                .ollamaCalls(aiUsageLogRepository.countByProvider(AiProvider.OLLAMA))
                .averageLatencyMs(aiUsageLogRepository.averageSuccessfulLatencyMs())
                .aiCallsLast7Days(aiUsageLogRepository.countByCreatedAtAfter(cutoff))
                .distinctAiUsers(aiUsageLogRepository.countDistinctUsers())
                .callsByModule(countCallsByModule())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiUsageLogResponse> getAiUsageLogs(Pageable pageable) {
        // Explicit ordering rather than relying on the caller's Sort: the log
        // view is only useful newest-first, and the created_at index backs it.
        return aiUsageLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toLogResponse);
    }

    // ---------- helpers ----------

    /**
     * Iterates AiModule rather than grouping in SQL so a module that has never
     * been called still reports 0 instead of being absent from the map -- the
     * admin panel should show "not used yet", not a missing row.
     */
    private Map<AiModule, Long> countCallsByModule() {
        Map<AiModule, Long> counts = new LinkedHashMap<>();
        for (AiModule module : AiModule.values()) {
            counts.put(module, aiUsageLogRepository.countByModule(module));
        }
        return counts;
    }

    private AiUsageLogResponse toLogResponse(AiUsageLog entry) {
        return AiUsageLogResponse.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .module(entry.getModule())
                .provider(entry.getProvider())
                .usedFallback(entry.isUsedFallback())
                .success(entry.isSuccess())
                .latencyMs(entry.getLatencyMs())
                .errorMessage(entry.getErrorMessage())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
