package com.proiect.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HallucinationGuardTest {

    private final HallucinationGuard guard = new HallucinationGuard();

    @Test
    void testNoHallucination() {
        String aiResponse = "Ai cheltuit 150.50 RON luna aceasta.";
        String toolOutput = "Total expenses: 150.50 RON";

        String result = guard.validate(aiResponse, toolOutput);

        assertEquals(aiResponse, result);
    }

    @Test
    void testHallucinationDetectedAndCorrected() {
        String aiResponse = "Ai cheltuit 200.00 RON luna aceasta.";
        String toolOutput = "Total expenses: 150.50 RON";

        String result = guard.validate(aiResponse, toolOutput);

        assertTrue(result.contains("150.50"));
        assertTrue(result.contains("Corrected by HallucinationGuard"));
    }

    @Test
    void testNoNumberInToolOutput() {
        String aiResponse = "Nu am găsit date.";
        String toolOutput = "No data available";

        String result = guard.validate(aiResponse, toolOutput);

        assertEquals(aiResponse, result);
    }

    @Test
    void testCommaDecimalSeparator() {
        String aiResponse = "Ai cheltuit 200,00 RON.";
        String toolOutput = "Total expenses: 150,50 RON";

        String result = guard.validate(aiResponse, toolOutput);

        assertTrue(result.contains("150.50"));
        assertTrue(result.contains("Corrected by HallucinationGuard"));
    }
}
