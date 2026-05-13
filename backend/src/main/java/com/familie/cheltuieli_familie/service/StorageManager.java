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

    private static final String DEFAULT_CURRENCY = "RON";
    private static final String DEFAULT_TYPE = "EXPENSE";
    private static final String SOURCE_TYPE = "OCR";

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
            if (saveTransaction(transaction)) {
                saved++;
            } else {
                failed++;
            }
        }

        return new StorageResult(transactions.size(), saved, failed);
    }

    private boolean saveTransaction(Transaction transaction) {
        try {
            if (!isValid(transaction)) {
                return false;
            }

            ExpenseOCREntity expense = createExpenseEntity(transaction);
            expenseRepository.save(expense);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private ExpenseOCREntity createExpenseEntity(Transaction transaction) {
        return new ExpenseOCREntity(
                BigDecimal.valueOf(transaction.getAmount()),
                transaction.getDescription(),
                resolveDate(transaction),
                resolveCurrency(transaction),
                resolveType(transaction),
                SOURCE_TYPE
        );
    }

    private LocalDateTime resolveDate(Transaction transaction) {
        if (transaction.getDate() == null) {
            return LocalDateTime.now();
        }

        return transaction.getDate().atStartOfDay();
    }

    private String resolveCurrency(Transaction transaction) {
        if (transaction.getCurrency() == null) {
            return DEFAULT_CURRENCY;
        }

        return transaction.getCurrency();
    }

    private String resolveType(Transaction transaction) {
        if (transaction.getType() == null) {
            return DEFAULT_TYPE;
        }

        return transaction.getType();
    }

    private boolean isValid(Transaction transaction) {
        return transaction != null
                && transaction.getAmount() > 0
                && transaction.getDescription() != null
                && !transaction.getDescription().trim().isEmpty();
    }
}