package com.proiect.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class EmbeddingConfig {

    @Value("${OPENAI_API_KEY:}")
    private String openaiApiKey;

    @Value("${OPENROUTER_API_KEY:}")
    private String openRouterApiKey;

    private static Map<String, String> loadDotEnv() {
        Path[] candidates = {
                Paths.get(".env"),
                Paths.get("..", ".env"),
                Paths.get(System.getProperty("user.dir"), ".env"),
                Paths.get(System.getProperty("user.dir"), "..", ".env")
        };
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return parseEnvFile(candidate);
            }
        }
        return new HashMap<>();
    }

    private static Map<String, String> parseEnvFile(Path path) {
        Map<String, String> envMap = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path)) {
                parseLine(line, envMap);
            }
        } catch (IOException e) {
            // ignore
        }
        return envMap;
    }

    private static void parseLine(String line, Map<String, String> envMap) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }
        int idx = trimmed.indexOf('=');
        if (idx > 0) {
            envMap.put(trimmed.substring(0, idx), trimmed.substring(idx + 1));
        }
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
    public EmbeddingModel embeddingModel() {
        String openaiKey = resolveKey(openaiApiKey, "OPENAI_API_KEY");
        if (!openaiKey.isEmpty()) {
            return OpenAiEmbeddingModel.builder()
                    .apiKey(openaiKey)
                    .modelName("text-embedding-3-small")
                    .dimensions(1536)
                    .build();
        }

        String openRouterKey = resolveKey(openRouterApiKey, "OPENROUTER_API_KEY");
        if (openRouterKey.isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY or OPENROUTER_API_KEY is required for embeddings.");
        }
        
        return OpenAiEmbeddingModel.builder()
                .apiKey(openRouterKey)
                .baseUrl("https://openrouter.ai/api/v1")
                .modelName("openai/text-embedding-3-small")
                .dimensions(1536)
                .build();
    }
}
