package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.ExpenseItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExpenseItemRepositoryTest {

    @Autowired
    private ExpenseItemRepository expenseItemRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    @DisplayName("Should generate an ID when an ExpenseItem is saved")
    void shouldGenerateIdWhenExpenseItemIsSaved() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setDate(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item = new ExpenseItem();
        item.setDescription("Bread");
        item.setAmount(BigDecimal.valueOf(5));
        item.setExpense(savedExpense);

        ExpenseItem savedItem = expenseItemRepository.save(item);

        assertNotNull(savedItem.getId());
    }

    @Test
    @DisplayName("Should find all ExpenseItems")
    void shouldFindAllExpenseItems() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(50));
        expense.setDate(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item1 = new ExpenseItem();
        item1.setDescription("Milk");
        item1.setAmount(BigDecimal.valueOf(10));
        item1.setExpense(savedExpense);

        ExpenseItem item2 = new ExpenseItem();
        item2.setDescription("Eggs");
        item2.setAmount(BigDecimal.valueOf(5));
        item2.setExpense(savedExpense);

        expenseItemRepository.save(item1);
        expenseItemRepository.save(item2);

        List<ExpenseItem> items = expenseItemRepository.findAll();

        List<String> itemDescriptions = items.stream()
                .map(ExpenseItem::getDescription)
                .collect(Collectors.toList());

        assertTrue(itemDescriptions.contains("Milk"));
        assertTrue(itemDescriptions.contains("Eggs"));

    }

    @Test
    @DisplayName("Should update ExpenseItem amount")
    void shouldUpdateExpenseItemAmount() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(70));
        expense.setDate(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item = new ExpenseItem();
        item.setDescription("Cheese");
        item.setAmount(BigDecimal.valueOf(15));
        item.setExpense(savedExpense);

        ExpenseItem savedItem = expenseItemRepository.save(item);
        savedItem.setAmount(BigDecimal.valueOf(20));

        ExpenseItem updatedItem = expenseItemRepository.save(savedItem);

        assertEquals(BigDecimal.valueOf(20), updatedItem.getAmount());
    }

    @Test
    @DisplayName("Should delete ExpenseItem")
    void shouldDeleteExpenseItem() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(90));
        expense.setDate(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item = new ExpenseItem();
        item.setDescription("Butter");
        item.setAmount(BigDecimal.valueOf(12));
        item.setExpense(savedExpense);

        ExpenseItem savedItem = expenseItemRepository.save(item);

        expenseItemRepository.delete(savedItem);

        Optional<ExpenseItem> deleted = expenseItemRepository.findById(savedItem.getId());
        assertTrue(deleted.isEmpty());
    }

    @Test
    @DisplayName("Should save ExpenseItem with Expense association")
    void shouldSaveExpenseItemWithExpenseAssociation() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(120));
        expense.setDate(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item = new ExpenseItem();
        item.setDescription("Juice");
        item.setAmount(BigDecimal.valueOf(8));
        item.setExpense(savedExpense);

        ExpenseItem savedItem = expenseItemRepository.save(item);

        assertEquals(savedExpense.getId(), savedItem.getExpense().getId());
    }
}