package com.familie.cheltuieli_familie.config;

import java.util.Map;

public final class KeyResolver {

    private KeyResolver() {
    }

    public static String resolve(String springValue, String envName) {
        String value = resolveFromSpringOrEnv(springValue, envName);
        if (!value.isEmpty()) {
            return value;
        }
        return resolveFromDotEnv(envName);
    }

    private static String resolveFromSpringOrEnv(String springValue, String envName) {
        if (springValue != null && !springValue.isEmpty()) {
            return springValue;
        }
        String env = System.getenv(envName);
        return env != null ? env : "";
    }

    private static String resolveFromDotEnv(String envName) {
        Map<String, String> dotEnv = DotEnvLoader.load();
        return dotEnv.getOrDefault(envName, "");
    }
}
