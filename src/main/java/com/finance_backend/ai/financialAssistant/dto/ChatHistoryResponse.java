package com.finance_backend.ai.financialAssistant.dto;

import com.finance_backend.ai.financialAssistant.entity.ChatMessageRole;

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
public class ChatHistoryResponse {

    private ChatMessageRole role;
    private String message;
    private LocalDateTime createdAt;
}