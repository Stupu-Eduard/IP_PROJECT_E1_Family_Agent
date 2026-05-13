package com.familie.cheltuieli_familie.config;

import com.familie.cheltuieli_familie.service.AnalyticsAssistant;
import com.familie.cheltuieli_familie.service.ExpenseTools;
import com.familie.cheltuieli_familie.service.HybridExpenseTool;
import com.familie.cheltuieli_familie.service.QdrantContentRetriever;
import com.familie.cheltuieli_familie.service.VisualIntentExtractor;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
@Slf4j
public class LlmConfig {

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKey;

    @Value("${OPENROUTER_API_KEY:}")
    private String openRouterApiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name:deepseek-chat}")
    private String deepseekModelName;

    @Value("${langchain4j.open-router.chat-model.base-url:https://openrouter.ai/api/v1}")
    private String openRouterBaseUrl;

    @Value("${langchain4j.open-router.chat-model.model-name:deepseek/deepseek-chat}")
    private String openRouterModelName;

    @Value("${langchain4j.open-ai.chat-model.temperature:0.1}")
    private double temperature;

    @Value("${langchain4j.open-ai.chat-model.timeout:60}")
    private long timeoutSeconds;

    @Value("${ai.intent-extraction.max-retries:3}")
    private int intentMaxRetries;

    @Value("${ai.intent-extraction.retry-delays-ms:2000,4000}")
    private String retryDelaysMsStr;

    @Value("${ai.intent-extraction.default-group-by:category}")
    private String defaultGroupBy;

    @Value("${ai.intent-extraction.default-series-by:person}")
    private String defaultSeriesBy;

    @Bean
    @Primary
    public ChatLanguageModel deepseekModel() {
        String dsKey = KeyResolver.resolve(deepseekApiKey, "DEEPSEEK_API_KEY");
        if (!dsKey.isEmpty()) {
            return buildOpenAiModel(dsKey, deepseekBaseUrl, deepseekModelName);
        }
        String orKey = KeyResolver.resolve(openRouterApiKey, "OPENROUTER_API_KEY");
        if (!orKey.isEmpty()) {
            return buildOpenAiModel(orKey, openRouterBaseUrl, openRouterModelName);
        }
        throw new IllegalStateException("DEEPSEEK_API_KEY or OPENROUTER_API_KEY is required.");
    }

    private ChatLanguageModel buildOpenAiModel(String apiKey, String baseUrl, String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Bean
    public RetrievalAugmentor retrievalAugmentor(QdrantContentRetriever qdrantContentRetriever) {
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(qdrantContentRetriever)
                .build();
    }

    public interface RagAssistant {
        @SystemMessage("""
            Ești un asistent virtual expert în managementul financiar al familiei, specializat în analiza cheltuielilor.
            
            INSTRUCȚIUNI DE OPERARE:
            1. Folosește EXCLUSIV contextul furnizat pentru a răspunde. Contextul conține fragmente din baza de date de cheltuieli (Qdrant).
            2. Dacă informația lipsește, răspunde politicos: "Nu am suficiente date în istoricul tău pentru a răspunde la această întrebare."
            3. Pentru întrebări complexe (analize, trenduri), structurează răspunsul clar, folosind liste sau puncte dacă este necesar.
            4. Menține un ton util, profesionist și prietenos.
            5. Răspunde întotdeauna în limba română.
            
            VALORI ȘI REGULI:
            - Acuratețea este prioritară. Nu inventa sume sau categorii.
            - Confidențialitatea: Datele aparțin familiei și trebuie tratate cu respect.
            - Claritatea: Explică orice calcul efectuat.
            """)
        String chat(String userMessage);
    }

    @Bean
    public RagAssistant ragAssistant(@Qualifier("deepseekModel") ChatLanguageModel deepseekModel, RetrievalAugmentor retrievalAugmentor) {
        return AiServices.builder(RagAssistant.class)
                .chatLanguageModel(deepseekModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
    }

    @Bean
    public AnalyticsAssistant analyticsAssistant(ChatLanguageModel deepseekModel, ExpenseTools expenseTools, HybridExpenseTool hybridExpenseTool) {
        return AiServices.builder(AnalyticsAssistant.class)
                .chatLanguageModel(deepseekModel)
                .tools(expenseTools, hybridExpenseTool)
                .build();
    }

    public interface ReportAssistant {
        @SystemMessage("""
            Ești un asistent financiar care generează rapoarte lunare narative pentru o familie.
            Misiunea ta este să transformi datele brute în analize ușor de înțeles:
            1. Rezumă activitatea financiară a lunii.
            2. Identifică variațiile mari (de exemplu: "ai cheltuit cu 15% mai mult pe divertisment").
            3. Oferă un sfat scurt și util pentru optimizarea cheltuielilor pe viitor.
            4. Tonul trebuie să fie prietenos, încurajator, dar profesionist.
            5. Răspunde EXCLUSIV în limba română.
            """)
        String generateReport(@UserMessage String aggregatedData);
    }

    @Bean
    public ReportAssistant reportAssistant(ChatLanguageModel deepseekModel) {
        return AiServices.builder(ReportAssistant.class)
                .chatLanguageModel(deepseekModel)
                .build();
    }

    public interface ConversationAssistant {
        @SystemMessage("""
            Ești FamilyAgent, un asistent virtual prietenos pentru managementul financiar al familiei.

            REGULI:
            1. Răspunde natural, prietenos și util.
            2. NU accesa baza de date — ești în mod conversație pură.
            3. Dacă utilizatorul cere date concrete (sume, grafice, analize), spune-i politicos
               că poate folosi comenzi precum "arată-mi cheltuielile pe categorie" sau "cât am cheltuit luna trecută".
            4. Răspunde întotdeauna în limba română.
            """)
        String chat(String userMessage);
    }

    @Bean
    public ConversationAssistant conversationAssistant(@Qualifier("deepseekModel") ChatLanguageModel deepseekModel) {
        return AiServices.builder(ConversationAssistant.class)
                .chatLanguageModel(deepseekModel)
                .build();
    }

    @Bean
    public VisualIntentExtractor visualIntentExtractor(ChatLanguageModel deepseekModel) {
        long[] retryDelaysMs = parseRetryDelays(retryDelaysMsStr, intentMaxRetries);
        return new VisualIntentExtractor(deepseekModel, intentMaxRetries, retryDelaysMs, defaultGroupBy, defaultSeriesBy);
    }

    private long[] parseRetryDelays(String str, int maxRetries) {
        long[] defaultDelays = new long[]{2000, 4000};
        long[] parsedDelays = parseLongArrayOrDefault(str, defaultDelays);
        return normalizeToLength(parsedDelays, maxRetries);
    }

    private long[] parseLongArrayOrDefault(String str, long[] defaultDelays) {
        if (str == null || str.isBlank()) {
            log.warn("Property ai.intent-extraction.retry-delays-ms is blank; using default retry delays.");
            return defaultDelays;
        }

        String[] parts = str.split(",");
        long[] temp = new long[parts.length];
        int count = 0;

        try {
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isBlank()) {
                    temp[count++] = Long.parseLong(trimmed);
                }
            }
        } catch (NumberFormatException ex) {
            log.warn("Invalid value '{}' for ai.intent-extraction.retry-delays-ms; using default retry delays.", str, ex);
            return defaultDelays;
        }

        if (count == 0) {
            log.warn("Property ai.intent-extraction.retry-delays-ms contains no valid values; using default retry delays.");
            return defaultDelays;
        }

        long[] result = new long[count];
        System.arraycopy(temp, 0, result, 0, count);
        return result;
    }

    private long[] normalizeToLength(long[] delays, int maxRetries) {
        if (maxRetries <= 0 || delays.length == maxRetries) {
            return delays;
        }

        long[] normalized = new long[maxRetries];
        int copyLength = Math.min(delays.length, maxRetries);
        System.arraycopy(delays, 0, normalized, 0, copyLength);

        long fillValue = delays[delays.length - 1];
        for (int i = copyLength; i < maxRetries; i++) {
            normalized[i] = fillValue;
        }

        if (delays.length != maxRetries) {
            log.warn(
                    "Configured retry delays count ({}) does not match ai.intent-extraction.max-retries ({}); normalized retry delays to match.",
                    delays.length,
                    maxRetries
            );
        }

        return normalized;
    }

}
