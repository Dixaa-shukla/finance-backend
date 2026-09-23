package com.finance_backend.ai.financialAssistant.service;

import com.finance_backend.ai.financialAssistant.dto.ChatHistoryResponse;
import com.finance_backend.ai.financialAssistant.dto.ChatRequest;
import com.finance_backend.ai.financialAssistant.dto.ChatResponse;

import java.util.List;

public interface ChatAssistantService {

    ChatResponse sendMessage(ChatRequest request);

    List<ChatHistoryResponse> getChatHistory(Long userId);

    void clearChatHistory(Long userId);
}