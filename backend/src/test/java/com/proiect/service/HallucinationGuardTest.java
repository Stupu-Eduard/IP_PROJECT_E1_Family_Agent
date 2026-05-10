package com.proiect.service;
import org.springframework.test.context.ContextConfiguration;

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
        assertTrue(result.contains("(Verificat și corectat automat"), "Response should indicate it was corrected");
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
        assertTrue(result.contains("(Verificat și corectat automat"), "Response should indicate it was corrected");
    }

    @Test
    void shouldCorrectNumericHallucination() {
        // GIVEN
        String aiResponse = "Ai cheltuit 150.00 RON pe utilități.";
        String toolOutput = "Total expenses: 200.00 RON";

        // WHEN
        String result = guard.validate(aiResponse, toolOutput);

        // THEN
        assertTrue(result.contains("200.00"), "The value 150.00 should have been replaced by 200.00");
    }
}
