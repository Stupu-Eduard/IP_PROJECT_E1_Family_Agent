package com.proiect.config;

import com.proiect.service.AnalyticsAssistant;
import com.proiect.service.ExpenseTools;
import com.proiect.service.QdrantContentRetriever;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
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

    @Value("${OPENAI_API_KEY:}")
    private String openaiApiKey;

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKey;

    @Value("${ANTHROPIC_API_KEY:}")
    private String anthropicApiKey;

    @Value("${OPENROUTER_API_KEY:}")
    private String openRouterApiKey;

    @Value("${COHERE_API_KEY:}")
    private String cohereApiKey;

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
                    return envMap;
                } catch (IOException e) {
                    // ignore
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
    public ChatLanguageModel deepseekModel() {
        String dsKey = resolveKey(deepseekApiKey, "DEEPSEEK_API_KEY");
        if (dsKey.isEmpty()) {
            String orKey = resolveKey(openRouterApiKey, "OPENROUTER_API_KEY");
            if (!orKey.isEmpty()) {
                return OpenAiChatModel.builder()
                        .apiKey(orKey)
                        .baseUrl("https://openrouter.ai/api/v1")
                        .modelName("deepseek/deepseek-chat")
                        .temperature(0.1)
                        .timeout(Duration.ofSeconds(60))
                        .build();
            }
            throw new IllegalStateException("DeepSeek API key or OpenRouter API key is required.");
        }
        return OpenAiChatModel.builder()
                .apiKey(dsKey)
                .baseUrl("https://api.deepseek.com")
                .modelName("deepseek-chat")
                .temperature(0.1)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public ChatLanguageModel claudeModel() {
        String antKey = resolveKey(anthropicApiKey, "ANTHROPIC_API_KEY");
        
        if (antKey.isEmpty()) {
            String orKey = resolveKey(openRouterApiKey, "OPENROUTER_API_KEY");
            if (!orKey.isEmpty()) {
                return OpenAiChatModel.builder()
                        .apiKey(orKey)
                        .baseUrl("https://openrouter.ai/api/v1")
                        .modelName("anthropic/claude-3.5-sonnet")
                        .temperature(0.1)
                        .timeout(Duration.ofSeconds(60))
                        .build();
            }
            throw new IllegalStateException("Anthropic API key or OpenRouter API key is required.");
        }
        
        // Note: customHeaders and Prompt Caching are not supported in LangChain4j 0.33.0
        return AnthropicChatModel.builder()
                .apiKey(antKey)
                .modelName("claude-3-5-sonnet-20240620")
                .temperature(0.1)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public ScoringModel scoringModel() {
        String key = resolveKey(cohereApiKey, "COHERE_API_KEY");
        if (key.isEmpty()) {
            throw new IllegalStateException("COHERE_API_KEY is required for re-ranking.");
        }
        return CohereScoringModel.builder()
                .apiKey(key)
                .modelName("rerank-multilingual-v3.0")
                .build();
    }

    @Bean
    public RetrievalAugmentor retrievalAugmentor(QdrantContentRetriever qdrantContentRetriever) {
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(qdrantContentRetriever)
                .build();
    }

    public interface RouterAssistant {
        @SystemMessage("""
            Ești un router de interogări pentru un sistem de management al cheltuielilor de familie.
            Misiunea ta este să clasifici mesajul utilizatorului în funcție de complexitatea sa.
            
            Categorii:
            1. SIMPLE:
               - Căutări de bază ("Cât am cheltuit ieri?")
               - Întrebări despre o singură cheltuială ("Unde am cumpărat pâine?")
               - Listări simple ("Arată-mi cheltuielile de la Lidl")
               - Întrebări factuale directe.
            
            2. COMPLEX:
               - Analize de trenduri ("Cum au evoluat cheltuielile pe mâncare în ultimele 3 luni?")
               - Comparații ("Am cheltuit mai mult luna asta decât luna trecută?")
               - Planificare bugetară ("Dacă continui așa, cât voi cheltui până la finalul anului?")
               - Întrebări care necesită corelarea mai multor date sau raționament matematic complex.
            
            Răspunde DOAR cu cuvântul 'SIMPLE' sau 'COMPLEX'.
            """)
        String classify(@UserMessage String userMessage);
    }

    @Bean
    public RouterAssistant routerAssistant(ChatLanguageModel deepseekModel) {
        return AiServices.builder(RouterAssistant.class)
                .chatLanguageModel(deepseekModel)
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
    public AnalyticsAssistant analyticsAssistant(ChatLanguageModel claudeModel, ExpenseTools expenseTools) {
        return AiServices.builder(AnalyticsAssistant.class)
                .chatLanguageModel(claudeModel)
                .tools(expenseTools)
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

    @Bean
    public dev.langchain4j.model.openai.OpenAiAudioModel whisperModel() {
        String key = resolveKey(openaiApiKey, "OPENAI_API_KEY");
        if (key.isEmpty()) {
            log.warn("OPENAI_API_KEY is missing. Whisper transcription will not work.");
        }
        return dev.langchain4j.model.openai.OpenAiAudioModel.builder()
                .apiKey(key)
                .modelName("whisper-1")
                .build();
    }
}
