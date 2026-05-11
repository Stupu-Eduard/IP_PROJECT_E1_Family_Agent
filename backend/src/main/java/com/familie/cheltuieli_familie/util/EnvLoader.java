package com.familie.cheltuieli_familie.util;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class EnvLoader {

    private EnvLoader() {
        // Utility class
    }

    public static String resolveKey(String springValue, String envName) {
        if (springValue != null && !springValue.isEmpty()) {
            return springValue;
        }
        String env = System.getenv(envName);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return loadDotEnv().getOrDefault(envName, "");
    }

    private static Map<String, String> loadDotEnv() {
        return Arrays.stream(new Path[]{
                        Paths.get(".env"),
                        Paths.get("..", ".env"),
                        Paths.get(System.getProperty("user.dir"), ".env"),
                        Paths.get(System.getProperty("user.dir"), "..", ".env")
                })
                .filter(Files::exists)
                .map(EnvLoader::parseEnvFile)
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
            log.error("Failed to read .env file at {}: {}", path, e.getMessage());
            return Collections.emptyMap();
        }
    }
}
