package com.familie.cheltuieli_familie.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DotEnvLoader {

    private static final Path[] CANDIDATES = new Path[]{
            Paths.get(".env"),
            Paths.get("..", ".env"),
            Paths.get(System.getProperty("user.dir"), ".env"),
            Paths.get(System.getProperty("user.dir"), "..", ".env")
    };

    private DotEnvLoader() {
    }

    public static Map<String, String> load() {
        for (Path candidate : CANDIDATES) {
            Map<String, String> envMap = tryLoad(candidate);
            if (!envMap.isEmpty()) {
                return envMap;
            }
        }
        return new HashMap<>();
    }

    private static Map<String, String> tryLoad(Path path) {
        if (!Files.exists(path)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, String> envMap = new HashMap<>();
            for (String line : Files.readAllLines(path)) {
                parseLine(line, envMap);
            }
            return envMap;
        } catch (IOException e) {
            return Collections.emptyMap();
        }
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
}
