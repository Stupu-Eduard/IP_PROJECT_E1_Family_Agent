package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BankingDictionaryCorrectorTest {

    private BankingDictionaryCorrector corrector;

    private BankingDictionaryCorrector initializeCorrector() {
        return new BankingDictionaryCorrector();
    }

    @BeforeEach
    void setUp() {
        corrector = initializeCorrector();
    }

    @Test
    void correctText_ShouldReturnNull_WhenTextIsNull() {
        assertNull(corrector.correctText(null));
    }

    @Test
    void correctText_ShouldReturnEmptyString_WhenTextIsEmpty() {
        assertEquals("", corrector.correctText(""));
    }

    @Test
    void correctText_ShouldNotChangeNumbersAndDates() {
        String input = "01/01/2026 100.50";
        String result = corrector.correctText(input);
        assertEquals("01/01/2026 100.50\n", result);
    }

    @Test
    void correctText_ShouldNotChangeIban() {
        String input = "RO24BTRL0000000000000000";
        String result = corrector.correctText(input);
        assertEquals("RO24BTRL0000000000000000\n", result);
    }

    @Test
    void correctText_ShouldCorrectTypo_WhenWordIsInDictionary() {
        String input = "EXTARS";
        String result = corrector.correctText(input);
        assertEquals("EXTRAS\n", result);
    }

    @Test
    void correctText_ShouldKeepWordAsIs_WhenWordIsTooShort() {
        String input = "de";
        String result = corrector.correctText(input);
        assertEquals("de\n", result);
    }

    @Test
    void correctText_ShouldKeepWordAsIs_WhenNoCloseMatchFound() {
        String input = "xyzabcde";
        String result = corrector.correctText(input);
        assertEquals("xyzabcde\n", result);
    }

    @Test
    void correctText_ShouldMatchCase_WhenValidWordIsFound() {
        String input = "extras CONT extrAS";
        String result = corrector.correctText(input);
        assertEquals("extras CONT extrAS\n", result);
    }
}
