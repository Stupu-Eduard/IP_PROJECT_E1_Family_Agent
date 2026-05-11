package com.familie.cheltuieli_familie.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class EmbeddingConfig {

    @Value("${OPENROUTER_API_KEY:}")
    private String openRouterApiKey;

    private static Map<String, String> loadDotEnv() {
        return Arrays.stream(new Path[]{
                        Paths.get(".env"),
                        Paths.get("..", ".env"),
                        Paths.get(System.getProperty("user.dir"), ".env"),
                        Paths.get(System.getProperty("user.dir"), "..", ".env")
                })
                .filter(Files::exists)
                .map(EmbeddingConfig::parseEnvFile)
                .filter(map -> !map.isEmpty())
                .findFirst()
                .orElse(Collections.emptyMap());
    }

    private static Map<String, String> parseEnvFile(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toMap(
                            parts -> parts[0].trim(),
                            parts -> parts[1].trim(),
                            (existing, replacement) -> existing
                    ));
        } catch (IOException e) {
            return Collections.emptyMap();
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
        String openRouterKey = resolveKey(openRouterApiKey, "OPENROUTER_API_KEY");
        if (openRouterKey.isEmpty()) {
            throw new IllegalStateException("OPENROUTER_API_KEY is required for embeddings.");
        }

        return OpenAiEmbeddingModel.builder()
                .apiKey(openRouterKey)
                .baseUrl("https://openrouter.ai/api/v1")
                .modelName("nvidia/llama-nemotron-embed-vl-1b-v2:free")
                .dimensions(2048)
                .build();
    }
}
