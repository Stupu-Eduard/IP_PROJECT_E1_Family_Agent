package com.proiect.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Value("${OPENROUTER_API_KEY:}")
    private String openRouterApiKey;

    @Bean
    public EmbeddingModel embeddingModel() {
        if (openRouterApiKey == null || openRouterApiKey.isEmpty()) {
            throw new IllegalStateException("OPENROUTER_API_KEY is required for embeddings. Please set it in the .env file.");
        }
        return OpenAiEmbeddingModel.builder()
                .apiKey(openRouterApiKey)
                .baseUrl("https://openrouter.ai/api/v1")
                .modelName("nvidia/llama-nemotron-embed-vl-1b-v2:free")
                .dimensions(2048)
                .build();
    }
}
