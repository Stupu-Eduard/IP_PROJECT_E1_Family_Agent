package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HallucinationGuardTest {

    private final HallucinationGuard guard = new HallucinationGuard();

    @Test
    void shouldCorrectSemanticHallucination_WhenToolSaysIncreaseButAiSaysDecrease() {
        // GIVEN
        String aiResponse = "Am observat o scădere de 20% la cheltuielile de mâncare.";
        String toolOutput = "Spending on Food has increased by 20.00% (50 RON)";

        // WHEN
        String result = guard.validate(aiResponse, toolOutput);

        // THEN
        assertTrue(result.contains("creștere"), "The word 'scădere' should have been replaced by 'creștere'");
        assertTrue(result.contains("corectat automat"), "Response should indicate it was corrected");
        assertFalse(result.contains("scădere"), "The word 'scădere' should not be present anymore");
    }

    @Test
    void shouldCorrectSemanticHallucination_WhenToolSaysDecreaseButAiSaysIncrease() {
        // GIVEN
        String aiResponse = "Cheltuielile tale au crescut luna aceasta.";
        String toolOutput = "Spending on Total has decreased by 15.00%";

        // WHEN
        String result = guard.validate(aiResponse, toolOutput);

        // THEN
        assertTrue(result.contains("scăzut"), "The word 'crescut' should have been replaced by 'scăzut'");
        assertTrue(result.contains("corectat automat"), "Response should indicate it was corrected");
    }

    @Test
    void shouldCorrectNumericHallucination() {
        // GIVEN
        String aiResponse = "Ai cheltuit 150.20 RON pe utilități.";
        String toolOutput = "Total expenses: 150.50 RON";

        // WHEN
        String result = guard.validate(aiResponse, toolOutput);

        // THEN
        assertTrue(result.contains("150.50"), "The value 150.20 should have been replaced by 150.50");
    }

    @Test
    void shouldNotCorrectWhenDiffIsOneOrMore() {
        // GIVEN
        String aiResponse = "Ai cheltuit 150.20 RON.";
        String toolOutput = "Total: 151.20 RON"; // Diff is exactly 1.00

        // WHEN
        String result = guard.validate(aiResponse, toolOutput);

        // THEN
        assertTrue(result.contains("150.20"), "Should NOT replace if diff >= 1.00");
    }

    @Test
    void shouldNotCorrectWhenInputTooLarge() {
        // GIVEN
        StringBuilder largeResponse = new StringBuilder();
        for (int i = 0; i < 2100; i++) largeResponse.append("a");
        String aiResponse = largeResponse.toString();
        String toolOutput = "150.50";

        // WHEN
        String result = guard.validate(aiResponse, toolOutput);

        // THEN
        assertEquals(aiResponse, result);
    }

    @Test
    void shouldHandleEmptyNumbersInToolOutput() {
        // GIVEN
        String aiResponse = "Ai cheltuit 150.20 RON.";
        String toolOutput = "No numbers here";

        // WHEN
        String result = guard.validate(aiResponse, toolOutput);

        // THEN
        assertEquals(aiResponse, result);
    }
}
