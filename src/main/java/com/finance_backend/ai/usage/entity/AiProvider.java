package com.finance_backend.ai.usage.entity;

/**
 * Which provider actually served a call. Mirrors the two ChatClient beans
 * wired in ai.config.ChatClientConfig, so a log row records the real provider
 * rather than just "primary" or "fallback" -- which of the two is primary is
 * configurable via finance.ai.primary-provider.
 */
public enum AiProvider {
    GEMINI,
    OLLAMA
}
