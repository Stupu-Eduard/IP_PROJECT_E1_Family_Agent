package com.familie.cheltuieli_familie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class HallucinationGuard {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d{1,10}[.,]\\d{1,10})");
    private static final int MAX_INPUT_LENGTH = 10000;
    private static final String SEMANTIC_INCREASE_EN = "increased";
    private static final String SEMANTIC_INCREASE_RO = "creștere";
    private static final String SEMANTIC_DECREASE_EN = "decreased";
    private static final String SEMANTIC_DECREASE_RO = "scădere";
    private static final String SEMANTIC_DECREASE_RO_ALT = "scăzut";
    private static final String SEMANTIC_LESS_RO = "mai puțin";
    private static final String SEMANTIC_MORE_RO = "mai mult";
    private static final String SEMANTIC_INCREASE_RO_ALT = "crescut";
    private static final String AUTO_CORRECTION_MSG = " (Verificat și corectat automat conform datelor brute din baza de date)";

    /**
     * Enhanced Validation: Verifică cifrele și trendul semantic (Fail-Safe).
     */
    public String validate(String aiResponse, String toolOutput) {
        log.info("Laura AI - Validare Enhanced Hallucination Guard...");
        
        String validatedResponse = validateNumbers(aiResponse, toolOutput);
        return validateSemantic(validatedResponse, toolOutput);
    }

    private String validateNumbers(String aiResponse, String toolOutput) {
        if (aiResponse != null && aiResponse.length() > MAX_INPUT_LENGTH) {
            log.warn("AI response exceeds max length, skipping number validation");
            return aiResponse;
        }
        if (toolOutput != null && toolOutput.length() > MAX_INPUT_LENGTH) {
            log.warn("Tool output exceeds max length, skipping number validation");
            return aiResponse;
        }

        List<BigDecimal> toolNumbers = new ArrayList<>();
        Matcher toolMatcher = NUMBER_PATTERN.matcher(toolOutput);
        while (toolMatcher.find()) {
            toolNumbers.add(new BigDecimal(toolMatcher.group(1).replace(",", ".")));
        }

        if (toolNumbers.isEmpty()) return aiResponse;

        Matcher aiMatcher = NUMBER_PATTERN.matcher(aiResponse);
        String correctedResponse = aiResponse;
        
        while (aiMatcher.find()) {
            try {
                BigDecimal aiValue = new BigDecimal(aiMatcher.group(1).replace(",", "."));
                BigDecimal closestToolValue = null;
                BigDecimal minDiff = null;

                for (BigDecimal toolValue : toolNumbers) {
                    BigDecimal diff = aiValue.subtract(toolValue).abs();
                    if (minDiff == null || diff.compareTo(minDiff) < 0) {
                        minDiff = diff;
                        closestToolValue = toolValue;
                    }
                }

                // Corecție automată dacă AI-ul a rotunjit greșit sau a halucinat cifra (sub 1 RON diferență)
                if (minDiff != null && minDiff.compareTo(BigDecimal.ZERO) > 0 && minDiff.compareTo(new BigDecimal("1.00")) < 0) {
                    correctedResponse = correctedResponse.replace(aiMatcher.group(1), closestToolValue.toPlainString());
                }
            } catch (Exception ignored) {}
        }
        return correctedResponse;
    }

    private String validateSemantic(String aiResponse, String toolOutput) {
        String toolLower = toolOutput.toLowerCase();
        boolean toolUp = toolLower.contains(SEMANTIC_INCREASE_EN) || toolLower.contains(SEMANTIC_INCREASE_RO);
        boolean toolDown = toolLower.contains(SEMANTIC_DECREASE_EN) || toolLower.contains(SEMANTIC_DECREASE_RO);

        if (!toolUp && !toolDown) return aiResponse;

        String correctedResponse = aiResponse;
        String lowerAi = aiResponse.toLowerCase();

        // Dacă tool-ul zice creștere, dar AI-ul folosește cuvinte de scădere (contradictie semantică)
        if (toolUp && (lowerAi.contains(SEMANTIC_DECREASE_RO) || lowerAi.contains(SEMANTIC_DECREASE_RO_ALT) || lowerAi.contains(SEMANTIC_LESS_RO))) {
            log.warn("Contradicție semantică detectată: Scădere vs Creștere. Corectare...");
            correctedResponse = aiResponse.replace(SEMANTIC_DECREASE_RO, SEMANTIC_INCREASE_RO)
                                          .replace(SEMANTIC_DECREASE_RO_ALT, SEMANTIC_INCREASE_RO_ALT)
                                          .replace(SEMANTIC_LESS_RO, SEMANTIC_MORE_RO);
        } 
        // Invers
        else if (toolDown && (lowerAi.contains(SEMANTIC_INCREASE_RO) || lowerAi.contains(SEMANTIC_INCREASE_RO_ALT) || lowerAi.contains(SEMANTIC_MORE_RO))) {
            log.warn("Contradicție semantică detectată: Creștere vs Scădere. Corectare...");
            correctedResponse = aiResponse.replace(SEMANTIC_INCREASE_RO, SEMANTIC_DECREASE_RO)
                                          .replace(SEMANTIC_INCREASE_RO_ALT, SEMANTIC_DECREASE_RO_ALT)
                                          .replace(SEMANTIC_MORE_RO, SEMANTIC_LESS_RO);
        }

        if (!correctedResponse.equals(aiResponse)) {
            correctedResponse += AUTO_CORRECTION_MSG;
        }
        return correctedResponse;
    }
}
