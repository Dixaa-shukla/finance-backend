package com.finance_backend.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class VectorStoreConfig {

    @Value("${finance.ai.primary-provider:gemini}")
    private String primaryProvider;

    @Value("${finance.ai.vector-store.persist-path:./data/vector-store.json}")
    private String persistPath;

    @Bean
    public VectorStore financialVectorStore(GoogleGenAiTextEmbeddingModel geminiEmbeddingModel,
                                            OllamaEmbeddingModel ollamaEmbeddingModel) {
        EmbeddingModel selected = isOllama(primaryProvider) ? ollamaEmbeddingModel : geminiEmbeddingModel;

        SimpleVectorStore store = SimpleVectorStore.builder(selected).build();

        File persistFile = new File(persistPath);
        if (persistFile.exists()) {
            store.load(persistFile);
        }

        return store;
    }

    private boolean isOllama(String provider) {
        return "ollama".equalsIgnoreCase(provider);
    }
}