package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BankingDictionaryCorrectorTest {

    private final BankingDictionaryCorrector corrector = new BankingDictionaryCorrector();

    @Test
    void correctTextShouldReturnSameTextForNull() {
        assertNull(corrector.correctText(null));
    }

    @Test
    void correctTextShouldReturnSameTextForEmptyText() {
        assertEquals("", corrector.correctText(""));
    }

    @Test
    void correctTextShouldCorrectKnownBankingWords() {
        String input = "EXTRS DE CONT";
        String result = corrector.correctText(input);

        assertTrue(result.contains("EXTRAS"));
        assertTrue(result.contains("CONT"));
    }

    @Test
    void correctTextShouldNotChangeNumbers() {
        String input = "Suma 100.50 RON";
        String result = corrector.correctText(input);

        assertTrue(result.contains("100.50"));
        assertTrue(result.contains("RON"));
    }

    @Test
    void correctTextShouldNotChangeDate() {
        String input = "10/03/2025 Lidl 100.50";
        String result = corrector.correctText(input);

        assertTrue(result.contains("10/03/2025"));
    }

    @Test
    void correctTextShouldKeepIbanLikeText() {
        String input = "RO49AAAA1B31007593840000";
        String result = corrector.correctText(input);

        assertTrue(result.contains("RO49AAAA1B31007593840000"));
    }
}
