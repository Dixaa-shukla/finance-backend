package com.finance_backend.ai.financialAssistant.controller;

import com.finance_backend.ai.financialAssistant.dto.ChatHistoryResponse;
import com.finance_backend.ai.financialAssistant.dto.ChatRequest;
import com.finance_backend.ai.financialAssistant.dto.ChatResponse;
import com.finance_backend.ai.financialAssistant.service.ChatAssistantService;
import com.finance_backend.ai.financialAssistant.service.TransactionIndexingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assistant")
public class ChatController {

    private final ChatAssistantService chatAssistantService;
    private final TransactionIndexingService transactionIndexingService;

    public ChatController(ChatAssistantService chatAssistantService,
                          TransactionIndexingService transactionIndexingService) {
        this.chatAssistantService = chatAssistantService;
        this.transactionIndexingService = transactionIndexingService;
    }

    /**
     * Send a message to the AI financial assistant. Automatically injects
     * the user's current expenses/budgets/goals/investments as context
     * (aggregate summary) PLUS the most semantically relevant individual
     * past transactions (RAG), and remembers the last 10 messages of conversation.
     * POST /api/v1/assistant/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatAssistantService.sendMessage(request));
    }

    /**
     * (Re)indexes a user's current Expense + Income records into the vector
     * store so RAG search has something to find. Call this after seeding or
     * changing a user's transaction data, before testing chat's transaction
     * recall. Not automatic yet -- see module design rationale.
     * POST /api/v1/assistant/reindex/{userId}
     */
    @PostMapping("/reindex/{userId}")
    public ResponseEntity<Map<String, Integer>> reindexUser(@PathVariable Long userId) {
        int count = transactionIndexingService.reindexUser(userId);
        return ResponseEntity.ok(Map.of("documentsIndexed", count));
    }

    /**
     * Full conversation history for a user, oldest first.
     * GET /api/v1/assistant/history/{userId}
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<ChatHistoryResponse>> getChatHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(chatAssistantService.getChatHistory(userId));
    }

    /**
     * Clear a user's conversation history (start fresh).
     * DELETE /api/v1/assistant/history/{userId}
     */
    @DeleteMapping("/history/{userId}")
    public ResponseEntity<Void> clearChatHistory(@PathVariable Long userId) {
        chatAssistantService.clearChatHistory(userId);
        return ResponseEntity.noContent().build();
    }   }