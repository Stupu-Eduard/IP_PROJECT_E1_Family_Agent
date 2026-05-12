package com.familie.cheltuieli_familie.validation;

import com.familie.cheltuieli_familie.model.Transaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TransactionValidatorTest {

    private final TransactionValidator validator = new TransactionValidator();

    @Test
    void validateShouldAcceptValidTransaction() {
        Transaction transaction = new Transaction(
                LocalDate.of(2026, 3, 10), // Data
                "Lidl",                    // Descriere
                100.50,                    // Suma
                "RON",                     // Moneda
                "EXPENSE"                  // Tipul
        );
        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    @Test
    void validateShouldRejectNullTransaction() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(null)
        );
        assertEquals("Transaction este null.", exception.getMessage());
    }

    @Test
    void validateShouldRejectMissingDate() {
        Transaction transaction = new Transaction(
                null,                      // Data lipseste
                "Lidl",
                100.50,
                "RON",
                "EXPENSE"
        );
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(transaction)
        );
        assertEquals("Transaction date lipseste.", exception.getMessage());
    }

    @Test
    void validateShouldRejectZeroAmount() {
        Transaction transaction = new Transaction(
                LocalDate.of(2026, 3, 10),
                "Lidl",
                0.0,                       // Suma este zero
                "RON",
                "EXPENSE"
        );
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(transaction)
        );
        assertEquals("Transaction amount nu poate fii 0.", exception.getMessage());
    }

    @Test
    void validateShouldRejectNullDescription() {
        Transaction transaction = new Transaction(
                LocalDate.of(2026, 3, 10),
                null,                      // Descrierea lipseste
                100.50,
                "RON",
                "EXPENSE"
        );
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(transaction)
        );
        assertEquals("Transaction description lipseste.", exception.getMessage());
    }

    @Test
    void validateShouldRejectBlankDescription() {
        Transaction transaction = new Transaction(
                LocalDate.of(2026, 3, 10),
                "   ",                     // Descrierea e spatiu gol
                100.50,
                "RON",
                "EXPENSE"
        );
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(transaction)
        );
        assertEquals("Transaction description lipseste.", exception.getMessage());
    }

    @Test
    void validateShouldRejectMissingType() {
        Transaction transaction = new Transaction(
                LocalDate.of(2026, 3, 10),
                "Lidl",
                100.50,
                "RON",
                null                       // Tipul lipseste
        );
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(transaction)
        );
        assertEquals("Transaction type lipseste.", exception.getMessage());
    }

    @Test
    void validateShouldRejectBlankType() {
        Transaction transaction = new Transaction(
                LocalDate.of(2026, 3, 10),
                "Lidl",
                100.50,
                "RON",
                "   "                      // Tipul e spatiu gol
        );
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(transaction)
        );
        assertEquals("Transaction type lipseste.", exception.getMessage());
    }

    @Test
    void validateShouldRejectMissingCurrency() {
        Transaction transaction = new Transaction(
                LocalDate.of(2026, 3, 10),
                "Lidl",
                100.50,
                null,                      // Moneda lipseste
                "EXPENSE"
        );
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(transaction)
        );
        assertEquals("Transaction currency lipseste.", exception.getMessage());
    }

    @Test
    void validateShouldRejectBlankCurrency() {
        Transaction transaction = new Transaction(
                LocalDate.of(2026, 3, 10),
                "Lidl",
                100.50,
                "   ",                     // Moneda e spatiu gol
                "EXPENSE"
        );
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(transaction)
        );
        assertEquals("Transaction currency lipseste.", exception.getMessage());
    }
}
