package com.finance_backend.admin.dto;

import com.finance_backend.ai.usage.entity.AiModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageSummaryResponse {

    // ================= ADOPTION (derived from feature tables) =================

    // ---- Financial Assistant chatbot (Module 11) ----
    private long totalChatMessages;
    private long totalUserMessages;
    /** ASSISTANT-role rows == number of AI responses served. */
    private long totalAiResponses;
    private long distinctChatbotUsers;
    private long chatMessagesLast7Days;

    // ---- Expense Categorization smart learning (Module 12) ----
    private long totalLearnedMerchants;
    private long distinctCategorizationUsers;

    // ---- AI Monthly Reports (Module 13) ----
    private long totalMonthlyReports;

    /**
     * Provider ATTEMPTS, not user requests: one request that fails over is two
     * calls here (a failed primary and a successful fallback).
     */
    private long totalAiCalls;
    private long successfulAiCalls;
    private long failedAiCalls;
    /** Attempts served by the secondary provider -- the fail-over rate. */
    private long fallbackAiCalls;

    private long geminiCalls;
    private long ollamaCalls;

    /** Averaged over successful calls only; null until one is recorded. */
    private Double averageLatencyMs;

    private long aiCallsLast7Days;
    private long distinctAiUsers;

    /** Every AiModule appears, */
    private Map<AiModule, Long> callsByModule;
}
