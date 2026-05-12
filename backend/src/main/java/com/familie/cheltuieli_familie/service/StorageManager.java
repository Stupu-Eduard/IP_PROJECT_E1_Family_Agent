package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.entity.ExpenseOCREntity;
import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.repository.ExpenseOCRRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StorageManager implements StorageService {

    private final ExpenseOCRRepository expenseRepository;

    public StorageManager(ExpenseOCRRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public StorageResult save(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return new StorageResult(0, 0, 0);
        }

        int saved = 0;
        int failed = 0;

        for (Transaction transaction : transactions) {
            try {
                if (!isValid(transaction)) {
                    failed++;
                    continue;
                }

                ExpenseOCREntity expense = new ExpenseOCREntity(
                        BigDecimal.valueOf(transaction.getAmount()),
                        transaction.getDescription(),
                        transaction.getDate() != null ? transaction.getDate().atStartOfDay() : LocalDateTime.now(),
                        transaction.getCurrency() != null ? transaction.getCurrency() : "RON",
                        transaction.getType() != null ? transaction.getType() : "EXPENSE",
                        "OCR"
                );

                expenseRepository.save(expense);
                saved++;

            } catch (Exception e) {
                failed++;
            }
        }

        return new StorageResult(transactions.size(), saved, failed);
    }

    private boolean isValid(Transaction transaction) {
        return transaction != null
                && transaction.getAmount() > 0
                && transaction.getDescription() != null
                && !transaction.getDescription().trim().isEmpty();
    }
}
