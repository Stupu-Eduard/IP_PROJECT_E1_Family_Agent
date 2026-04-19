package com.proiect.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class HallucinationGuard {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+[.,]\\d+)");

    /**
     * Compares numbers in the AI response with the actual figure from the tool.
     * If they differ, it corrects the response.
     */
    public String validate(String aiResponse, String toolOutput) {
        log.info("Validating AI response against tool output.");
        
        Matcher toolMatcher = NUMBER_PATTERN.matcher(toolOutput);
        if (!toolMatcher.find()) {
            return aiResponse; // No number in tool output to compare
        }
        
        String toolValue = toolMatcher.group(1).replace(",", ".");
        double actualValue = Double.parseDouble(toolValue);

        Matcher aiMatcher = NUMBER_PATTERN.matcher(aiResponse);
        while (aiMatcher.find()) {
            String aiValueStr = aiMatcher.group(1).replace(",", ".");
            try {
                double aiValue = Double.parseDouble(aiValueStr);
                // Check if they are significantly different (more than 0.01)
                if (Math.abs(aiValue - actualValue) > 0.01) {
                    log.warn("Hallucination detected! AI said {}, Tool said {}. Correcting...", aiValue, actualValue);
                    return aiResponse.replace(aiMatcher.group(1), toolValue) + " (Corrected by HallucinationGuard)";
                }
            } catch (NumberFormatException e) {
                // Ignore non-parseable matches
            }
        }

        return aiResponse;
    }
}
