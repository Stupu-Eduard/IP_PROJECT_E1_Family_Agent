package com.familie.cheltuieli_familie.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Value("${OPENROUTER_API_KEY:}")
    private String openRouterApiKey;

    @Value("${langchain4j.open-router.embedding-model.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${langchain4j.open-router.embedding-model.model-name:nvidia/llama-nemotron-embed-vl-1b-v2:free}")
    private String modelName;

    @Value("${langchain4j.open-router.embedding-model.dimensions:2048}")
    private int dimensions;

    @Bean
    public EmbeddingModel embeddingModel() {
        String openRouterKey = KeyResolver.resolve(openRouterApiKey, "OPENROUTER_API_KEY");
        if (openRouterKey.isEmpty()) {
            throw new IllegalStateException("OPENROUTER_API_KEY is required for embeddings.");
        }

        return OpenAiEmbeddingModel.builder()
                .apiKey(openRouterKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .dimensions(dimensions)
                .build();
    }
}
