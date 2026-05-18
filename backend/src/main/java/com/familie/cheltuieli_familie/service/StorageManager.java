package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.entity.ExpenseOCREntity;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.repository.ExpenseOCRRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class StorageManager implements StorageService {

    private static final String DEFAULT_CURRENCY = "RON";
    private static final String DEFAULT_TYPE = "EXPENSE";
    private static final String SOURCE_TYPE = "OCR";

    private final ExpenseOCRRepository expenseRepository;
    private final SyncService syncService;

    public StorageManager(ExpenseOCRRepository expenseRepository, SyncService syncService) {
        this.expenseRepository = expenseRepository;
        this.syncService = syncService;
    }

    @Override
    public StorageResult save(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return new StorageResult(0, 0, 0);
        }

        int saved = 0;
        int failed = 0;

        for (Transaction transaction : transactions) {
            if (saveAndSyncTransaction(transaction)) {
                saved++;
            } else {
                failed++;
            }
        }

        return new StorageResult(transactions.size(), saved, failed);
    }

    private boolean saveAndSyncTransaction(Transaction transaction) {
        try {
            if (!isValid(transaction)) {
                return false;
            }

            ExpenseOCREntity expense = createExpenseEntity(transaction);
            expenseRepository.save(expense);

            // Also sync to Qdrant via ExpenseEntity for RAG context
            syncToVectorStore(expense);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private void syncToVectorStore(ExpenseOCREntity ocrExpense) {
        try {
            ExpenseEntity entity = ExpenseEntity.builder()
                    .id(ocrExpense.getId())
                    .amount(ocrExpense.getAmount())
                    .category(ocrExpense.getDescription())  // Use description as category fallback
                    .location("OCR")
                    .person("OCR")
                    .date(ocrExpense.getDate() != null ? ocrExpense.getDate().toLocalDate() : null)
                    .rawInput(ocrExpense.getDescription())
                    .build();
            syncService.syncExpense(entity);
        } catch (Exception e) {
            // Log but don't fail the main save
            log.error("Failed to sync OCR expense to vector store: {}", e.getMessage());
        }
    }

    private ExpenseOCREntity createExpenseEntity(Transaction transaction) {
        ExpenseOCREntity expense = new ExpenseOCREntity();
        expense.setAmount(BigDecimal.valueOf(transaction.getAmount()));
        expense.setDescription(transaction.getDescription());
        expense.setDate(resolveDate(transaction));
        expense.setCurrency(resolveCurrency(transaction));
        expense.setTransactionType(resolveType(transaction));
        expense.setSourceType(SOURCE_TYPE);
        expense.setReceiptUrl(transaction.getReceiptUrl());
        return expense;
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