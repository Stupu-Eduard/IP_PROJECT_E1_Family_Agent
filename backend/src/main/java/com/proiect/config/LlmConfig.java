package com.proiect.config;

import com.proiect.service.AnalyticsAssistant;
import com.proiect.service.ExpenseTools;
import com.proiect.service.QdrantContentRetriever;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
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
                    .modelName("nvidia/nemotron-4-340b-instruct") 
                    .temperature(0.1)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        } else {
            throw new IllegalStateException("Nu s-a găsit niciun API Key pentru LLM în variabilele de mediu.");
        }
    }

    @Bean
    public RetrievalAugmentor retrievalAugmentor(QdrantContentRetriever qdrantContentRetriever) {
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(qdrantContentRetriever)
                .build();
    }

    public interface RagAssistant {
        @SystemMessage("""
            Ești un asistent de cheltuieli de familie. Răspunde la întrebările despre cheltuieli folosind DOAR contextul furnizat.
            Dacă contextul nu conține suficiente informații pentru a răspunde exact la întrebare, spune "Nu am suficiente date."
            Răspunde în limba română.
            """)
        String chat(String userMessage);
    }

    @Bean
    public RagAssistant ragAssistant(ChatLanguageModel chatLanguageModel, RetrievalAugmentor retrievalAugmentor) {
        return AiServices.builder(RagAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
    }

    @Bean
    public AnalyticsAssistant analyticsAssistant(ChatLanguageModel chatLanguageModel, ExpenseTools expenseTools) {
        return AiServices.builder(AnalyticsAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(expenseTools)
                .build();
    }
}
