package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HallucinationGuardTest {

    private final HallucinationGuard guard = new HallucinationGuard();

    @Test
    void shouldCorrectSemanticHallucination_WhenToolSaysIncreaseButAiSaysDecrease() {
        String aiResponse = "Am observat o scădere de 20% la cheltuielile de mâncare.";
        String toolOutput = "Spending on Food has increased by 20.00% (50 RON)";

        String result = guard.validate(aiResponse, toolOutput);

        assertTrue(result.contains("creștere"), "The word 'scădere' should have been replaced by 'creștere'");
        assertTrue(result.contains("corectat automat"), "Response should indicate it was corrected");
        assertFalse(result.contains("scădere"), "The word 'scădere' should not be present anymore");
    }

    @Test
    void shouldCorrectSemanticHallucination_WhenToolSaysDecreaseButAiSaysIncrease() {
        String aiResponse = "Cheltuielile tale au crescut luna aceasta.";
        String toolOutput = "Spending on Total has decreased by 15.00%";

        String result = guard.validate(aiResponse, toolOutput);

        assertTrue(result.contains("scăzut"), "The word 'crescut' should have been replaced by 'scăzut'");
        assertTrue(result.contains("corectat automat"), "Response should indicate it was corrected");
    }

    @Test
    void shouldCorrectNumericHallucination() {
        String aiResponse = "Ai cheltuit 150.20 RON pe utilități.";
        String toolOutput = "Total expenses: 150.50 RON";

        String result = guard.validate(aiResponse, toolOutput);

        assertTrue(result.contains("150.50"), "The value 150.20 should have been replaced by 150.50");
    }

    // ---- Numeric validation edge cases ----

    @Test
    void shouldHandleNullAiResponseWhenToolHasNoTrend() {
        String toolOutput = "Plain report without any trend words";

        String result = guard.validate(null, toolOutput);

        assertNull(result, "Should return null when aiResponse is null and tool has no trend");
    }

    @Test
    void shouldHandleNullToolOutput() {
        String aiResponse = "Ai cheltuit 100.00 RON.";

        String result = guard.validate(aiResponse, null);

        assertEquals(aiResponse, result, "Should return aiResponse unchanged when toolOutput is null");
    }

    @Test
    void shouldSkipNumberValidationWhenAiResponseTooLong() {
        String longResponse = "x".repeat(10001);
        String toolOutput = "Total: 100.00 RON";

        String result = guard.validate(longResponse, toolOutput);

        assertEquals(longResponse, result, "Should return aiResponse unchanged when it exceeds max length");
    }

    @Test
    void shouldSkipNumberValidationWhenToolOutputTooLong() {
        String aiResponse = "Ai cheltuit 100.00 RON.";
        String longToolOutput = "x".repeat(10001);

        String result = guard.validate(aiResponse, longToolOutput);

        assertEquals(aiResponse, result, "Should return aiResponse unchanged when tool output exceeds max length");
    }

    @Test
    void shouldReturnUnchangedWhenToolOutputHasNoNumbers() {
        String aiResponse = "Ai cheltuit 150.50 RON.";
        String toolOutput = "Nu există cifre aici";

        String result = guard.validate(aiResponse, toolOutput);

        assertEquals(aiResponse, result, "Should return unchanged when tool output contains no numbers");
    }

    @Test
    void shouldNotCorrectWhenExactNumericMatch() {
        String aiResponse = "Ai cheltuit 150.50 RON.";
        String toolOutput = "Total: 150.50 RON";

        String result = guard.validate(aiResponse, toolOutput);

        assertEquals(aiResponse, result, "Should not change when numbers match exactly");
    }

    @Test
    void shouldNotCorrectWhenNumericDiffExceedsOne() {
        String aiResponse = "Ai cheltuit 150.50 RON.";
        String toolOutput = "Total: 152.00 RON";

        String result = guard.validate(aiResponse, toolOutput);

        assertEquals(aiResponse, result, "Should not correct when difference is 1.00 or more");
    }

    @Test
    void shouldCorrectUsingClosestNumberAmongMultipleToolValues() {
        String aiResponse = "Ai cheltuit 150.50 RON.";
        String toolOutput = "Values: 149.80, 150.20, 200.00";

        String result = guard.validate(aiResponse, toolOutput);

        assertTrue(result.contains("150.20"), "Should replace with closest tool value (diff=0.30)");
    }

    // ---- Semantic validation edge cases ----

    @Test
    void shouldNotChangeWhenToolHasNoTrendKeywords() {
        String aiResponse = "Am observat o scădere la cheltuieli.";
        String toolOutput = "Plain report without any trend words";

        String result = guard.validate(aiResponse, toolOutput);

        assertEquals(aiResponse, result, "Should not change when tool output has no trend keywords");
    }

    @Test
    void shouldNotChangeWhenAiMatchesToolTrend() {
        String aiResponse = "Cheltuielile au înregistrat o creștere.";
        String toolOutput = "A fost observată o creștere de 10%";

        String result = guard.validate(aiResponse, toolOutput);

        assertEquals(aiResponse, result, "Should not change when AI matches tool trend");
        assertFalse(result.contains("corectat automat"), "Should not add correction message");
    }

    @Test
    void shouldCorrectWhenToolDownRomanianAndAiSaysCrestere() {
        String aiResponse = "Cheltuielile au înregistrat o creștere semnificativă.";
        String toolOutput = "S-a observat o scădere de 5%";

        String result = guard.validate(aiResponse, toolOutput);

        assertTrue(result.contains("scădere"), "Should replace 'creștere' with 'scădere'");
        assertTrue(result.contains("corectat automat"), "Should indicate correction");
    }

    @Test
    void shouldCorrectWhenToolUpAndAiSaysScazut() {
        String aiResponse = "Valoarea a scăzut.";
        String toolOutput = "The value has increased";

        String result = guard.validate(aiResponse, toolOutput);

        assertTrue(result.contains("crescut"), "Should replace 'scăzut' with 'crescut'");
        assertTrue(result.contains("corectat automat"), "Should indicate correction");
    }

    @Test
    void shouldCorrectWhenToolUpAndAiSaysMaiPutin() {
        String aiResponse = "Am cheltuit mai puțin decât luna trecută.";
        String toolOutput = "Spending has increased this month";

        String result = guard.validate(aiResponse, toolOutput);

        assertTrue(result.contains("mai mult"), "Should replace 'mai puțin' with 'mai mult'");
        assertTrue(result.contains("corectat automat"), "Should indicate correction");
    }

    @Test
    void shouldCorrectWhenToolDownAndAiSaysMaiMult() {
        String aiResponse = "Am cheltuit mai mult decât luna trecută.";
        String toolOutput = "Spending has decreased this month";

        String result = guard.validate(aiResponse, toolOutput);

        assertTrue(result.contains("mai puțin"), "Should replace 'mai mult' with 'mai puțin'");
        assertTrue(result.contains("corectat automat"), "Should indicate correction");
    }

    @Test
    void shouldCorrectWhenToolDownAndAiSaysCrestere() {
        String aiResponse = "Cheltuielile au înregistrat o creștere.";
        String toolOutput = "Spending has decreased by 10%";

        String result = guard.validate(aiResponse, toolOutput);

        assertTrue(result.contains("scădere"), "Should replace 'creștere' with 'scădere'");
        assertTrue(result.contains("corectat automat"), "Should indicate correction");
    }

    @Test
    void shouldNotChangeWhenToolDownButAiDoesNotContradict() {
        String aiResponse = "Cheltuielile sunt stabile.";
        String toolOutput = "Spending has decreased by 10%";

        String result = guard.validate(aiResponse, toolOutput);

        assertEquals(aiResponse, result, "Should not change when AI does not contradict tool trend");
        assertFalse(result.contains("corectat automat"), "Should not add correction message");
    }
}
