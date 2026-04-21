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
    public EmbeddingModel embeddingModel() {
        String key = resolveKey(openRouterApiKey, "OPENROUTER_API_KEY");
        if (key.isEmpty()) {
            throw new IllegalStateException("OPENROUTER_API_KEY is required for embeddings. Please set it in the .env file or as environment variable.");
        }
        return OpenAiEmbeddingModel.builder()
                .apiKey(key)
                .baseUrl("https://openrouter.ai/api/v1")
                .modelName("nvidia/llama-nemotron-embed-vl-1b-v2:free")
                .dimensions(2048)
                .build();
    }
}
