package com.familie.cheltuieli_familie.config;

import java.util.Map;

public final class KeyResolver {

    private KeyResolver() {
    }

    public static String resolve(String springValue, String envName) {
        if (springValue != null && !springValue.isEmpty()) {
            return springValue;
        }
        String env = System.getenv(envName);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        Map<String, String> dotEnv = DotEnvLoader.load();
        return dotEnv.getOrDefault(envName, "");
    }
}
