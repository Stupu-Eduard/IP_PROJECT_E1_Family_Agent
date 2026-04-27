package com.familie.cheltuieli_familie.service;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.Properties;
import java.io.FileInputStream;
import java.nio.file.Paths;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Tag("integration")
public class ApiConnectivityTest {

    @Test
    public void testBothApis() {
        System.out.println("--- TESTING API CONNECTIVITY ---");
        
        // Read keys manually since we're not starting full Spring Boot with external .env loader
        String openRouterKey = System.getenv("OPENROUTER_API_KEY");
        String deepseekKey = System.getenv("DEEPSEEK_API_KEY");
        
        try (InputStream input = new FileInputStream(Paths.get(".env").toFile())) {
            Properties prop = new Properties();
            prop.load(input);
            if (openRouterKey == null || openRouterKey.isEmpty()) {
                openRouterKey = prop.getProperty("OPENROUTER_API_KEY");
            }
            if (deepseekKey == null || deepseekKey.isEmpty()) {
                deepseekKey = prop.getProperty("DEEPSEEK_API_KEY");
            }
        } catch (Exception ex) {
            System.out.println("Could not load .env file directly, relying on system env only.");
        }

        // 1. DeepSeek Test
        if (deepseekKey != null && !deepseekKey.trim().isEmpty()) {
            try {
                System.out.println("\nTesting DeepSeek API...");
                ChatLanguageModel deepseekModel = OpenAiChatModel.builder()
                        .apiKey(deepseekKey)
                        .baseUrl("https://api.deepseek.com")
                        .modelName("deepseek-chat")
                        .build();
                
                String dsResponse = deepseekModel.generate("Hello, are you receiving this message? Reply only with 'DeepSeek confirms reception.'");
                System.out.println("DeepSeek Response -> " + dsResponse);
            } catch (Exception e) {
                System.out.println("DeepSeek Test Failed: " + e.getMessage());
            }
        } else {
            System.out.println("\nDeepSeek API Key not found.");
        }

        // 2. OpenRouter (Nemotron3) Test
        if (openRouterKey != null && !openRouterKey.trim().isEmpty()) {
            try {
                System.out.println("\nTesting OpenRouter API...");
                ChatLanguageModel openRouterModel = OpenAiChatModel.builder()
                        .apiKey(openRouterKey)
                        .baseUrl("https://openrouter.ai/api/v1")
                        .modelName("nvidia/nemotron-4-340b-instruct")
                        .build();
                
                String orResponse = openRouterModel.generate("Hello, are you receiving this message? Reply only with 'OpenRouter confirms reception.'");
                System.out.println("OpenRouter Response -> " + orResponse);
            } catch (Exception e) {
                System.out.println("OpenRouter Test Failed: " + e.getMessage());
            }
        } else {
            System.out.println("\nOpenRouter API Key not found.");
        }
        
        System.out.println("\n--- END OF TESTS ---");
    }
}
