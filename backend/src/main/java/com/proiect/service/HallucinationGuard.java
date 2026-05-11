package com.proiect.service;

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

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+[.,]\\d+)");
    private static final String TEXT_CRESTERE = "creștere";
    private static final String TEXT_SCADERE = "scădere";
    private static final String TEXT_INCREASED = "increased";
    private static final String TEXT_DECREASED = "decreased";
    private static final String TEXT_SCAZUT = "scăzut";
    private static final String TEXT_MAI_PUTIN = "mai puțin";
    private static final String TEXT_CRESCUT = "crescut";
    private static final String TEXT_MAI_MULT = "mai mult";

    /**
     * Enhanced Validation: Verifică cifrele și trendul semantic (Fail-Safe).
     */
    public String validate(String aiResponse, String toolOutput) {
        log.info("Laura AI - Validare Enhanced Hallucination Guard...");
        
        String validatedResponse = validateNumbers(aiResponse, toolOutput);
        return validateSemantic(validatedResponse, toolOutput);
    }

    private String validateNumbers(String aiResponse, String toolOutput) {
        List<BigDecimal> toolNumbers = extractNumbers(toolOutput);
        if (toolNumbers.isEmpty()) return aiResponse;

        Matcher aiMatcher = NUMBER_PATTERN.matcher(aiResponse);
        String correctedResponse = aiResponse;
        
        while (aiMatcher.find()) {
            try {
                String aiMatch = aiMatcher.group(1);
                BigDecimal aiValue = new BigDecimal(aiMatch.replace(",", "."));
                BigDecimal closestToolValue = findClosestValue(aiValue, toolNumbers);

                if (closestToolValue != null) {
                    BigDecimal diff = aiValue.subtract(closestToolValue).abs();
                    // Corecție automată sub 100 RON (conform logicii existente)
                    if (diff.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(new BigDecimal("100.00")) < 0) {
                        correctedResponse = correctedResponse.replace(aiMatch, closestToolValue.toPlainString());
                    }
                }
            } catch (Exception ignored) {
                // Ignorăm formatele de numere invalide în timpul extragerii sau corecției
                log.debug("Format numeric invalid detectat: {}", ignored.getMessage());
            }
        }
        return correctedResponse;
    }

    private List<BigDecimal> extractNumbers(String text) {
        List<BigDecimal> numbers = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                numbers.add(new BigDecimal(matcher.group(1).replace(",", ".")));
            } catch (Exception ignored) {
                // Ignorăm formatele de numere invalide în timpul extragerii sau corecției
                log.debug("Format numeric invalid detectat: {}", ignored.getMessage());
            }
        }
        return numbers;
    }

    private BigDecimal findClosestValue(BigDecimal aiValue, List<BigDecimal> toolNumbers) {
        BigDecimal closestToolValue = null;
        BigDecimal minDiff = null;

        for (BigDecimal toolValue : toolNumbers) {
            BigDecimal diff = aiValue.subtract(toolValue).abs();
            if (minDiff == null || diff.compareTo(minDiff) < 0) {
                minDiff = diff;
                closestToolValue = toolValue;
            }
        }
        return closestToolValue;
    }

    private String validateSemantic(String aiResponse, String toolOutput) {
        String lowerTool = toolOutput.toLowerCase();
        boolean toolUp = lowerTool.contains(TEXT_INCREASED) || lowerTool.contains(TEXT_CRESTERE);
        boolean toolDown = lowerTool.contains(TEXT_DECREASED) || lowerTool.contains(TEXT_SCADERE);

        if (!toolUp && !toolDown) return aiResponse;

        String correctedResponse = aiResponse;
        String lowerAi = aiResponse.toLowerCase();

        // Dacă tool-ul zice creștere, dar AI-ul folosește cuvinte de scădere (contradictie semantică)
        if (toolUp && (lowerAi.contains(TEXT_SCADERE) || lowerAi.contains(TEXT_SCAZUT) || lowerAi.contains(TEXT_MAI_PUTIN))) {
            log.warn("Contradicție semantică detectată: Scădere vs Creștere. Corectare...");
            correctedResponse = aiResponse.replace(TEXT_SCADERE, TEXT_CRESTERE)
                                          .replace(TEXT_SCAZUT, TEXT_CRESCUT)
                                          .replace(TEXT_MAI_PUTIN, TEXT_MAI_MULT);
        } 
        // Invers
        else if (toolDown && (lowerAi.contains(TEXT_CRESTERE) || lowerAi.contains(TEXT_CRESCUT) || lowerAi.contains(TEXT_MAI_MULT))) {
            log.warn("Contradicție semantică detectată: Creștere vs Scădere. Corectare...");
            correctedResponse = aiResponse.replace(TEXT_CRESTERE, TEXT_SCADERE)
                                          .replace(TEXT_CRESCUT, TEXT_SCAZUT)
                                          .replace(TEXT_MAI_MULT, TEXT_MAI_PUTIN);
        }

        if (!correctedResponse.equals(aiResponse)) {
            correctedResponse += " (Verificat și corectat automat conform datelor brute din baza de date)";
        }
        return correctedResponse;
    }
}
