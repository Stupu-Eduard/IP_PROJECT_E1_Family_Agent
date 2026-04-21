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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class LlmConfig {

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKey;

    @Value("${OPENROUTER_API_KEY:}")
    private String openRouterApiKey;

    private static Map<String, String> loadDotEnv() {
        Map<String, String> envMap = new HashMap<>();
        Path[] candidates = new Path[]{
                Paths.get(".env"),
                Paths.get("..", ".env"),
                Paths.get(System.getProperty("user.dir"), ".env"),
                Paths.get(System.getProperty("user.dir"), "..", ".env")
        };
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                try {
                    for (String line : Files.readAllLines(candidate)) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int idx = line.indexOf('=');
                        if (idx > 0) {
                            envMap.put(line.substring(0, idx), line.substring(idx + 1));
                        }
                    }
                    log.info("Loaded .env from: {}", candidate.toAbsolutePath());
                    return envMap;
                } catch (IOException e) {
                    log.warn("Failed to read .env from {}: {}", candidate, e.getMessage());
                }
            }
        }
        return envMap;
    }

    private String resolveKey(String springValue, String envName) {
        if (springValue != null && !springValue.isEmpty()) {
            return springValue;
        }
        String env = System.getenv(envName);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return loadDotEnv().getOrDefault(envName, "");
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        String dsKey = resolveKey(deepseekApiKey, "DEEPSEEK_API_KEY");
        String orKey = resolveKey(openRouterApiKey, "OPENROUTER_API_KEY");
        if (!dsKey.isEmpty()) {
            return OpenAiChatModel.builder()
                    .apiKey(dsKey)
                    .baseUrl("https://api.deepseek.com")
                    .modelName("deepseek-chat")
                    .temperature(0.1)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        } else if (!orKey.isEmpty()) {
            return OpenAiChatModel.builder()
                    .apiKey(orKey)
                    .baseUrl("https://openrouter.ai/api/v1")
                    .modelName("nvidia/nemotron-4-340b-instruct")
                    .temperature(0.1)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        } else {
            throw new IllegalStateException("Nu s-a găsit niciun API Key pentru LLM în variabilele de mediu sau în fișierul .env.");
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
