package com.finance_backend.ai.financialAssistant.service;

import com.finance_backend.ai.financialAssistant.dto.ChatHistoryResponse;
import com.finance_backend.ai.financialAssistant.dto.ChatRequest;
import com.finance_backend.ai.financialAssistant.dto.ChatResponse;
import com.finance_backend.ai.financialAssistant.entity.ChatHistory;
import com.finance_backend.ai.financialAssistant.entity.ChatMessageRole;
import com.finance_backend.ai.financialAssistant.repository.ChatHistoryRepository;
import com.finance_backend.ai.usage.entity.AiModule;
import com.finance_backend.ai.usage.service.AiUsageTrackingService;
import com.finance_backend.common.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * configured primary provider (Gemini by default) first; if that
 * call throws for any reason, automatically retries once against the
 * fallback provider (Ollama by default) before giving up.
 */
@Slf4j
@Service
public class ChatAssistantServiceImpl implements ChatAssistantService {

    private static final int RAG_TOP_K = 5;

    /** Identifies this service's calls in the ai_usage_log table. */
    private static final AiModule AI_MODULE = AiModule.FINANCIAL_ASSISTANT;

    private static final String SYSTEM_INSTRUCTIONS = """
            You are a helpful, India-savvy personal finance assistant embedded in a
            personal finance management app. Use INR (rupees, denoted Rs.) for all
            amounts unless the user's data says otherwise. Base your answers ONLY on
            the financial summary and relevant transactions given to you below --
            never invent numbers that aren't in it. Be concise, practical, and
            encouraging. If asked about something outside personal finance, politely
            redirect back to finance topics.
            """;

    private final ChatClient primaryChatClient;
    private final ChatClient fallbackChatClient;
    private final ChatHistoryRepository chatHistoryRepository;
    private final FinancialContextBuilder financialContextBuilder;
    private final VectorStore vectorStore;
    private final AiUsageTrackingService aiUsageTrackingService;

    public ChatAssistantServiceImpl(@Qualifier("primaryChatClient") ChatClient primaryChatClient,
                                    @Qualifier("fallbackChatClient") ChatClient fallbackChatClient,
                                    ChatHistoryRepository chatHistoryRepository,
                                    FinancialContextBuilder financialContextBuilder,
                                    VectorStore vectorStore,
                                    AiUsageTrackingService aiUsageTrackingService) {
        this.primaryChatClient = primaryChatClient;
        this.fallbackChatClient = fallbackChatClient;
        this.chatHistoryRepository = chatHistoryRepository;
        this.financialContextBuilder = financialContextBuilder;
        this.vectorStore = vectorStore;
        this.aiUsageTrackingService = aiUsageTrackingService;
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        String aggregateContext = financialContextBuilder.buildContext(request.getUserId());
        String relevantTransactionsContext = searchRelevantTransactions(request.getUserId(), request.getMessage());

        String fullContext = aggregateContext + "\n\n" + relevantTransactionsContext;
        List<Message> promptMessages = buildPromptMessages(request.getUserId(), fullContext, request.getMessage());

        saveMessage(request.getUserId(), ChatMessageRole.USER, request.getMessage());

        String reply = callWithFallback(request.getUserId(), new Prompt(promptMessages));

        saveMessage(request.getUserId(), ChatMessageRole.ASSISTANT, reply);

        return ChatResponse.builder()
                .reply(reply)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> getChatHistory(Long userId) {
        return chatHistoryRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(h -> ChatHistoryResponse.builder()
                        .role(h.getRole())
                        .message(h.getMessage())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void clearChatHistory(Long userId) {
        chatHistoryRepository.deleteByUserId(userId);
    }

    // ---------- helpers ----------

    /** RAG retrieval step: semantic search over this user's indexed transactions, scoped by a metadata filter. */
    private String searchRelevantTransactions(Long userId, String userMessage) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(userMessage)
                    .topK(RAG_TOP_K)
                    .filterExpression(b.eq("userId", userId).build())
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            if (results == null || results.isEmpty()) {
                return "Relevant past transactions: none found (the user may not have indexed their "
                        + "transactions yet via POST /api/v1/assistant/reindex/" + userId + ").";
            }

            StringBuilder sb = new StringBuilder("Relevant past transactions (semantic search results):\n");
            for (Document doc : results) {
                sb.append("- ").append(doc.getText()).append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            // RAG is an enrichment, not the core value -- a search failure
            // must not break the whole chat response. Degrade gracefully.
            log.warn("Vector store similarity search failed for userId={}, continuing without it", userId, e);
            return "Relevant past transactions: unavailable right now.";
        }
    }

    /**
     * userId is carried in purely so each attempt can be attributed in
     * ai.usage -- it has no effect on the prompt or the fallback decision.
     */
    private String callWithFallback(Long userId, Prompt prompt) {
        long startedAt = System.nanoTime();
        try {
            String reply = primaryChatClient.prompt(prompt).call().content();
            aiUsageTrackingService.recordSuccess(AI_MODULE, userId, false, elapsedMs(startedAt));
            return reply;
        } catch (Exception primaryFailure) {
            aiUsageTrackingService.recordFailure(AI_MODULE, userId, false, elapsedMs(startedAt),
                    primaryFailure.getMessage());
            log.warn("Primary AI provider failed, retrying with fallback provider", primaryFailure);
            long fallbackStartedAt = System.nanoTime();
            try {
                String reply = fallbackChatClient.prompt(prompt).call().content();
                aiUsageTrackingService.recordSuccess(AI_MODULE, userId, true, elapsedMs(fallbackStartedAt));
                return reply;
            } catch (Exception fallbackFailure) {
                aiUsageTrackingService.recordFailure(AI_MODULE, userId, true, elapsedMs(fallbackStartedAt),
                        fallbackFailure.getMessage());
                log.error("Fallback AI provider also failed", fallbackFailure);
                throw new ExternalServiceException(
                        "AI assistant is temporarily unavailable. Please try again later.", fallbackFailure);
            }
        }
    }

    /** nanoTime, not currentTimeMillis: only the former is monotonic. */
    private static long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    private List<Message> buildPromptMessages(Long userId, String fullContext, String currentUserMessage) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_INSTRUCTIONS + "\n\n" + fullContext));

        List<ChatHistory> recentHistory = chatHistoryRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        Collections.reverse(recentHistory);

        for (ChatHistory h : recentHistory) {
            if (h.getRole() == ChatMessageRole.USER) {
                messages.add(new UserMessage(h.getMessage()));
            } else {
                messages.add(new AssistantMessage(h.getMessage()));
            }
        }

        messages.add(new UserMessage(currentUserMessage));
        return messages;
    }

    private void saveMessage(Long userId, ChatMessageRole role, String message) {
        ChatHistory entry = ChatHistory.builder()
                .userId(userId)
                .role(role)
                .message(message)
                .build();
        chatHistoryRepository.save(entry);
    }
}