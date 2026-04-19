package com.proiect.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LlmConfig {

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKey;

    @Value("${OPENROUTER_API_KEY:}")
    private String openRouterApiKey;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // Prefer DeepSeek if available, otherwise fallback to OpenRouter (Nemotron3)
        // Adjust the base URL and model name based on the provider
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            return OpenAiChatModel.builder()
                    .apiKey(deepseekApiKey)
                    .baseUrl("https://api.deepseek.com")
                    .modelName("deepseek-chat")
                    .temperature(0.1)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        } else if (openRouterApiKey != null && !openRouterApiKey.isEmpty()) {
            return OpenAiChatModel.builder()
                    .apiKey(openRouterApiKey)
                    .baseUrl("https://openrouter.ai/api/v1")
                    .modelName("nvidia/nemotron-3-super-120b-a12b:free") // or similar nemotron3 available in openrouter
                    .temperature(0.1)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        } else {
            throw new IllegalStateException("Nu s-a găsit niciun API Key pentru LLM în variabilele de mediu.");
        }
    }
}
