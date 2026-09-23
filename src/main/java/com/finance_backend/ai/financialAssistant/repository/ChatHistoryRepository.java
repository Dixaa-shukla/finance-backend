package com.finance_backend.ai.financialAssistant.repository;

import com.finance_backend.ai.financialAssistant.entity.ChatHistory;
import com.finance_backend.ai.financialAssistant.entity.ChatMessageRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    /** Full history, oldest first -- used for the "view past conversation" endpoint. */
    List<ChatHistory> findByUserIdOrderByCreatedAtAsc(Long userId);

    /** Most recent N messages, newest first -- used to build prompt context (see ChatAssistantServiceImpl). */
    List<ChatHistory> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByUserId(Long userId);

    // ---------- Module 16 (Admin Panel) -- AI Usage Monitoring ----------
    // There is no dedicated AI usage-tracking table yet, so chatbot usage is
    // derived from this table.
    /** Messages by role -- USER = prompts received, ASSISTANT = AI responses served. */
    long countByRole(ChatMessageRole role);

    /** Per-user message count, for the admin user detail view. */
    long countByUserId(Long userId);

    /** Recent activity, e.g. "messages in the last 7 days". */
    long countByCreatedAtAfter(LocalDateTime cutoff);

    /** How many distinct users have actually used the chatbot. */
    @Query("SELECT COUNT(DISTINCT c.userId) FROM ChatHistory c")
    long countDistinctUsers();
}