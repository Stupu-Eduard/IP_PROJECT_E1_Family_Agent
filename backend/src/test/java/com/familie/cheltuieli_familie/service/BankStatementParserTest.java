package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankStatementParserTest {

    private BankStatementParser parser;

    private BankStatementParser initializeParser() {
        return new BankStatementParser();
    }

    @BeforeEach
    void setUp() {
        parser = initializeParser();
    }

    @Test
    void parseText_ShouldReturnEmptyList_WhenInputIsNull() {
        List<Transaction> result = parser.parseText(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseText_ShouldReturnEmptyList_WhenInputIsEmptyString() {
        List<Transaction> result = parser.parseText("");
        assertTrue(result.isEmpty());
    }

    @Test
    void parseText_ShouldIgnoreLine_WhenNoDateFound() {
        String ocrText = "Linie oarecare fara data";
        List<Transaction> result = parser.parseText(ocrText);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseText_ShouldIgnoreLine_WhenDateIsInvalid() {
        String ocrText = "35/13/2026 Descriere 100.00";
        List<Transaction> result = parser.parseText(ocrText);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseText_ShouldIgnoreLine_WhenAmountIsZeroOrNegative() {
        String ocrText = "01/01/2026 Descriere 0.00";
        List<Transaction> result = parser.parseText(ocrText);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseText_ShouldParseCorrectLine_WhenInputIsValid() {
        String ocrText = "01/01/2026 Cumparaturi Mega 150.50";
        List<Transaction> result = parser.parseText(ocrText);
        assertEquals(1, result.size());
        Transaction transaction = result.get(0);
        assertEquals("2026-01-01", transaction.getDate());
        assertEquals(150.5, transaction.getAmount());
        assertEquals("Cumparaturi Mega", transaction.getDescription());
    }

    @Test
    void parseText_ShouldReplaceCharacterAndParse_WhenInputContainsOInsteadOfZero() {
        String ocrText = "01/01/2026 Cumparaturi Mega 150.5O";
        List<Transaction> result = parser.parseText(ocrText);
        assertEquals(1, result.size());
        assertEquals(150.5, result.get(0).getAmount());
    }
}
