package com.familie.cheltuieli_familie.validation;

import com.familie.cheltuieli_familie.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionValidatorTest {

    private TransactionValidator validator;

    private TransactionValidator createValidator() {
        return new TransactionValidator();
    }

    @BeforeEach
    void setUp() {
        validator = createValidator();
    }

    @Test
    void validate_ShouldThrowException_WhenTransactionIsNull() {
        assertThrows(ValidationException.class, () -> {
            validator.validate(null);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenDateIsBlank() {
        Transaction transaction = new Transaction(null, 100.0, "Description", "expense", "RON");

        assertThrows(ValidationException.class, () -> {
            validator.validate(transaction);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenAmountIsZero() {
        Transaction transaction = new Transaction("2026-01-01", 0.0, "Description", "expense", "RON");

        assertThrows(ValidationException.class, () -> {
            validator.validate(transaction);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenDescriptionIsBlank() {
        Transaction transaction = new Transaction("2026-01-01", 100.0, "", "expense", "RON");

        assertThrows(ValidationException.class, () -> {
            validator.validate(transaction);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenTypeIsBlank() {
        Transaction transaction = new Transaction("2026-01-01", 100.0, "Description", "", "RON");

        assertThrows(ValidationException.class, () -> {
            validator.validate(transaction);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenCurrencyIsBlank() {
        Transaction transaction = new Transaction("2026-01-01", 100.0, "Description", "expense", "");

        assertThrows(ValidationException.class, () -> {
            validator.validate(transaction);
        });
    }

    @Test
    void validate_ShouldNotThrowException_WhenTransactionIsValid() {
        Transaction transaction = new Transaction("2026-01-01", 100.0, "Description", "expense", "RON");

        assertDoesNotThrow(() -> {
            validator.validate(transaction);
        });
    }
}
