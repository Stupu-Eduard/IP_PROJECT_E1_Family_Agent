package com.familie.cheltuieli_familie.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        Map<String, String> envMap = new HashMap<>();
        for (Path candidate : CANDIDATES) {
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
}
