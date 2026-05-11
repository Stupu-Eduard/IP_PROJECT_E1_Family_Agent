package com.familie.cheltuieli_familie.validation;

import com.familie.cheltuieli_familie.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionValidator {

    public void validate(Transaction transaction) throws ValidationException {
        if (transaction == null) {
            throw new ValidationException("Transaction este null.");
        }

        if (transaction.getDate() == null) {
            throw new ValidationException("Transaction date lipseste.");
        }

        if (transaction.getAmount() == 0) {
            throw new ValidationException("Transaction amount nu poate fii 0.");
        }

        if (isBlank(transaction.getDescription())) {
            throw new ValidationException("Transaction description lipseste.");
        }

        if (isBlank(transaction.getType())) {
            throw new ValidationException("Transaction type lipseste.");
        }

        if (isBlank(transaction.getCurrency())) {
            throw new ValidationException("Transaction currency lipseste.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}