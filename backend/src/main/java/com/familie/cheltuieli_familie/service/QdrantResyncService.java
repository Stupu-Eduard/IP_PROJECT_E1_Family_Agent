package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.mapper.ExpenseMapper;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class QdrantResyncService {

    private static final int LOG_PROGRESS_INTERVAL = 100;

    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper expenseMapper;
    private final QdrantVectorService qdrantVectorService;

    public QdrantResyncService(ExpenseRepository expenseRepository,
                               ExpenseMapper expenseMapper,
                               QdrantVectorService qdrantVectorService) {
        this.expenseRepository = expenseRepository;
        this.expenseMapper = expenseMapper;
        this.qdrantVectorService = qdrantVectorService;
    }

    public ResyncResult resyncAllExpenses() {
        return resyncExpenses(expenseRepository.findAll());
    }

    public ResyncResult resyncExpensesForFamily(Long familyId) {
        if (familyId == null) {
            throw new IllegalArgumentException("familyId must not be null");
        }
        return resyncExpenses(expenseRepository.findAllByFamilyId(familyId));
    }

    private ResyncResult resyncExpenses(List<Expense> expenses) {
        int processed = 0;
        int errors = 0;

        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);
            try {
                ExpenseEntity entity = expenseMapper.toExpenseEntity(expense);
                qdrantVectorService.storeExpense(entity);
                processed++;
            } catch (Exception e) {
                log.error("Failed to resync expense ID {}: {}", expense.getId(), e.getMessage());
                errors++;
            }

            if ((i + 1) % LOG_PROGRESS_INTERVAL == 0) {
                log.info("Resynced {}/{} expenses", i + 1, expenses.size());
            }
        }

        log.info("Resync completed. Processed: {}, Errors: {}", processed, errors);
        return new ResyncResult(processed, errors);
    }

    public record ResyncResult(int processedCount, int errorCount) {
    }
}
