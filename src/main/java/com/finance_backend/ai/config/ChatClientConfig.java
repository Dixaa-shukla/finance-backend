package com.finance_backend.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Value("${finance.ai.primary-provider:gemini}")
    private String primaryProvider;

    @Bean
    public ChatClient primaryChatClient(GoogleGenAiChatModel geminiModel, OllamaChatModel ollamaModel) {
        return ChatClient.builder(resolve(primaryProvider, geminiModel, ollamaModel)).build();
    }

    @Bean
    public ChatClient fallbackChatClient(GoogleGenAiChatModel geminiModel, OllamaChatModel ollamaModel) {
        String fallbackProvider = isOllama(primaryProvider) ? "gemini" : "ollama";
        return ChatClient.builder(resolve(fallbackProvider, geminiModel, ollamaModel)).build();
    }

    private ChatModel resolve(String provider, GoogleGenAiChatModel geminiModel, OllamaChatModel ollamaModel) {
        return isOllama(provider) ? ollamaModel : geminiModel;
    }

    private boolean isOllama(String provider) {
        return "ollama".equalsIgnoreCase(provider);
    }
}