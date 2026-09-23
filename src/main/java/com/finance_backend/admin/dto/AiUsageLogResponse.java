package com.finance_backend.admin.dto;

import com.finance_backend.ai.usage.entity.AiModule;
import com.finance_backend.ai.usage.entity.AiProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageLogResponse {

    private Long id;
    /** Null for scheduler-driven calls, which belong to no single user. */
    private Long userId;
    private AiModule module;
    private AiProvider provider;
    /** true if this attempt was the retry against the secondary provider. */
    private boolean usedFallback;
    private boolean success;
    /** Duration of this attempt alone, not of the whole user request. */
    private Long latencyMs;
    /** Provider error text (truncated to 500 chars); null on success. */
    private String errorMessage;
    private LocalDateTime createdAt;
}
