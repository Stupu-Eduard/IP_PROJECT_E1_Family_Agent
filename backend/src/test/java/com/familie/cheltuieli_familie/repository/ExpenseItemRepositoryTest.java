package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Category;
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
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    @DisplayName("Should generate an ID when an ExpenseItem is saved")
    void shouldGenerateIdWhenExpenseItemIsSaved() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setExpense_date(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item = new ExpenseItem();
        item.setItemName("Bread");
        item.setAmount(BigDecimal.valueOf(5));
        item.setQuantity(BigDecimal.valueOf(2));
        item.setExpense(savedExpense);

        ExpenseItem savedItem = expenseItemRepository.save(item);

        assertNotNull(savedItem.getId());
    }

    @Test
    @DisplayName("Should find all ExpenseItems")
    void shouldFindAllExpenseItems() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(50));
        expense.setExpense_date(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item1 = new ExpenseItem();
        item1.setItemName("Milk");
        item1.setAmount(BigDecimal.valueOf(10));
        item1.setExpense(savedExpense);

        ExpenseItem item2 = new ExpenseItem();
        item2.setItemName("Eggs");
        item2.setAmount(BigDecimal.valueOf(5));
        item2.setExpense(savedExpense);

        expenseItemRepository.save(item1);
        expenseItemRepository.save(item2);

        List<ExpenseItem> items = expenseItemRepository.findAll();

        List<String> itemNames = items.stream()
                .map(ExpenseItem::getItemName)
                .collect(Collectors.toList());

        assertTrue(itemNames.contains("Milk"));
        assertTrue(itemNames.contains("Eggs"));

    }

    @Test
    @DisplayName("Should update ExpenseItem amount")
    void shouldUpdateExpenseItemAmount() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(70));
        expense.setExpense_date(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item = new ExpenseItem();
        item.setItemName("Cheese");
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
        expense.setExpense_date(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item = new ExpenseItem();
        item.setItemName("Butter");
        item.setAmount(BigDecimal.valueOf(12));
        item.setExpense(savedExpense);

        ExpenseItem savedItem = expenseItemRepository.save(item);

        expenseItemRepository.delete(savedItem);

        Optional<ExpenseItem> deleted = expenseItemRepository.findById(savedItem.getId());
        assertTrue(deleted.isEmpty());
    }

    @Test
    @DisplayName("Should NOT delete parent Expense when ExpenseItem is deleted")
    void shouldNotDeleteExpenseWhenItemIsDeleted() {
        // Arrange
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.TEN);
        expense.setExpense_date(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item = new ExpenseItem();
        item.setItemName("Temporary Item");
        item.setAmount(BigDecimal.TEN);
        item.setExpense(savedExpense);
        ExpenseItem savedItem = expenseItemRepository.save(item);

        // Act
        expenseItemRepository.delete(savedItem);

        // Assert
        assertTrue(expenseRepository.findById(savedExpense.getId()).isPresent(),
                "Parent Expense should still exist!");
    }

    @Test
    @DisplayName("Should save ExpenseItem with Expense association")
    void shouldSaveExpenseItemWithExpenseAssociation() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(120));
        expense.setExpense_date(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item = new ExpenseItem();
        item.setItemName("Juice");
        item.setAmount(BigDecimal.valueOf(8));
        item.setExpense(savedExpense);

        ExpenseItem savedItem = expenseItemRepository.save(item);

        assertEquals(savedExpense.getId(), savedItem.getExpense().getId());
    }

    @Test
    @DisplayName("Should save ExpenseItem with Category association")
    void shouldSaveExpenseItemWithCategory() {
        // Arrange
        Category category = new Category();
        category.setName("Dairy");
        Category savedCategory = categoryRepository.save(category);

        ExpenseItem item = new ExpenseItem();
        item.setItemName("Yogurt");
        item.setAmount(BigDecimal.valueOf(4));
        item.setCategory(savedCategory);

        // Act
        ExpenseItem savedItem = expenseItemRepository.save(item);

        // Assert
        assertNotNull(savedItem.getCategory());
        assertEquals("Dairy", savedItem.getCategory().getName());
    }

    @Test
    @DisplayName("Should have default quantity as 1 when not specified")
    void shouldHaveDefaultQuantity() {
        // Arrange
        ExpenseItem item = new ExpenseItem();
        item.setItemName("Test Item");
        item.setAmount(BigDecimal.TEN);

        // Act
        ExpenseItem savedItem = expenseItemRepository.save(item);

        // Assert
        assertEquals(0, BigDecimal.ONE.compareTo(savedItem.getQuantity()));
    }

    @Test
    @DisplayName("Constraint: Should correctly save decimal quantities")
    void shouldSaveDecimalQuantity() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(20));
        Expense savedExpense = expenseRepository.save(expense);

        ExpenseItem item = new ExpenseItem();
        item.setItemName("Mere");
        item.setAmount(BigDecimal.valueOf(15.5));
        item.setQuantity(BigDecimal.valueOf(2.50)); // 2.5 kg
        item.setExpense(savedExpense);

        ExpenseItem savedItem = expenseItemRepository.save(item);

        assertEquals(0, BigDecimal.valueOf(2.50).compareTo(savedItem.getQuantity()));
    }
}