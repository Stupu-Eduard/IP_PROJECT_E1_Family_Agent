package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreSyncService {

    private final ExpenseRepository expenseRepository;
    private final QdrantVectorService qdrantVectorService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncMissingExpenses() {
        log.info("Starting vector store sync for missing expenses...");
        List<Expense> allExpenses = expenseRepository.findAll();
        int synced = 0;
        int skipped = 0;
        int failed = 0;

        for (Expense expense : allExpenses) {
            try {
                boolean exists = qdrantVectorService.existsInVectorStore(expense.getId());
                if (exists) {
                    skipped++;
                    continue;
                }

                ExpenseEntity entity = toExpenseEntity(expense);
                qdrantVectorService.storeExpense(entity);
                synced++;
                log.debug("Synced expense ID {} to vector store", expense.getId());
            } catch (Exception e) {
                failed++;
                log.error("Failed to sync expense ID {}: {}", expense.getId(), e.getMessage());
            }
        }

        log.info("Vector store sync complete. Synced: {}, Skipped: {}, Failed: {}", synced, skipped, failed);
    }

    private ExpenseEntity toExpenseEntity(Expense expense) {
        ExpenseEntity entity = new ExpenseEntity();
        entity.setId(expense.getId());
        entity.setAmount(expense.getAmount());
        entity.setCategory(expense.getCategory() != null ? expense.getCategory().getName() : null);
        entity.setLocation(expense.getLocation() != null ? expense.getLocation().getStore() : null);
        entity.setPerson(expense.getUser() != null ? expense.getUser().getName() : null);
        entity.setDate(expense.getExpenseDate() != null ? expense.getExpenseDate().toLocalDate() : null);

        String rawInput = expense.getRawInput();
        if (rawInput == null || rawInput.isBlank()) {
            rawInput = String.format("Cheltuială %s: %s, Sumă: %s RON, Categorie: %s, Magazin: %s, Persoană: %s",
                    expense.getSourceType(), expense.getDescription(), expense.getAmount(),
                    expense.getCategory() != null ? expense.getCategory().getName() : null,
                    expense.getLocation() != null ? expense.getLocation().getStore() : null,
                    expense.getUser() != null ? expense.getUser().getName() : null);
        }
        entity.setRawInput(rawInput);
        entity.setCreatedAt(expense.getCreatedAt() != null ? expense.getCreatedAt() : LocalDateTime.now());
        return entity;
    }
}
