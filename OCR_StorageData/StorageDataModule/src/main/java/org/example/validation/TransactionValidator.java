package org.example.validation;

import org.example.model.Transaction;

public class TransactionValidator {

    public void validate(Transaction transaction) throws ValidationException {
        if (transaction == null) {
            throw new ValidationException("Transaction este null.");
        }

        if (isBlank(transaction.getDate())) {
            throw new ValidationException("Transaction date lipseste.");
        }

        if (transaction.getAmount() == 0) {
            throw new ValidationException("Transactia nu poate fii  0.");
        }

        if (isBlank(transaction.getDescription())) {
            throw new ValidationException("Descrierea tranzactiei lipseste.");
        }

        if (isBlank(transaction.getType())) {
            throw new ValidationException("Tipul tranzactiei lipseste.");
        }

        if (isBlank(transaction.getCurrency())) {
            throw new ValidationException("Transaction currency lipseste.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}