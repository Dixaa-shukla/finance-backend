package com.finance_backend.ai.usage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Every AI service tries the primary provider and retries against
 * Ollama, so a SINGLE user action can produce TWO rows: a
 * success=false row for the provider that failed and a success=true row for
 * recovered.

 * The token columns are reserved and intentionally left null. Populating them
 * would mean replacing the services' `.call().content()` / `.call().entity(..)`
 * chains with `.call().chatResponse()` .
 */
@Entity
@Table(name = "ai_usage_log", indexes = {
        @Index(name = "idx_ai_usage_log_user_id", columnList = "user_id"),
        @Index(name = "idx_ai_usage_log_module", columnList = "module"),
        @Index(name = "idx_ai_usage_log_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable: scheduler-driven calls are not always attributable to a user. */
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 30)
    private AiModule module;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 10)
    private AiProvider provider;

    /** true when this row is the retry against the secondary provider. */
    @Column(name = "used_fallback", nullable = false)
    private boolean usedFallback;

    @Column(name = "success", nullable = false)
    private boolean success;

    /** Wall-clock time for this single provider attempt, not the whole request. */
    @Column(name = "latency_ms")
    private Long latencyMs;

    // ---- reserved for future token accounting; see class Javadoc ----

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    /** Provider error text, truncated to fit the column; null on success. */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
